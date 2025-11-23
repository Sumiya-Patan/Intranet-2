package com.intranet.service.cornjobs.CompleteWeekHoliday;

import com.intranet.dto.HolidayDTO;
import com.intranet.dto.lms.LeaveDTO;
import com.intranet.entity.HolidayExcludeUsers;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.WeekInfo;
import com.intranet.entity.WeeklyTimeSheetReview;
import com.intranet.repository.HolidayExcludeUsersRepo;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.WeekInfoRepo;
import com.intranet.repository.WeeklyTimeSheetReviewRepo;
import com.intranet.service.email.ums_corn_job_token.UmsAuthService;
import com.intranet.util.cache.LeaveDirectoryService;
import com.intranet.util.cache.UserDirectoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FullHolidayWeekProcessorService {

    private final UserDirectoryService userDirectoryService;
    private final UmsAuthService umsAuthService;
    private final WeekInfoRepo weekInfoRepo;
    private final HolidayExcludeUsersRepo holidayExcludeUsersRepo;
    private final LeaveDirectoryService leaveDirectoryService;

    private final TimeSheetRepo timeSheetRepo;
    private final WeeklyTimeSheetReviewRepo weeklyReviewRepo;

    private final RestTemplate restTemplate = new RestTemplate();


    @Value("${lms.api.base-url}")
    private String lmsBaseUrl;

    @Transactional
    public void processMonth() {

        LocalDate currentMonth = LocalDate.now();
        LocalDate previousMonth = currentMonth.minusMonths(1);
        int month = previousMonth.getMonthValue();
        int year = previousMonth.getYear();

        String token = umsAuthService.getUmsToken();
        String authHeader = "Bearer " + token;

        // -------------------------------
        // 1️⃣ Fetch ALL USERS from UMS
        // -------------------------------
        Map<Long, Map<String, Object>> users = userDirectoryService.fetchAllUsers(authHeader);

        if (users.isEmpty()) {
            System.err.println("⚠ No users from UMS → EXITING CRON");
            return;
        }

        // -------------------------------
        // 2️⃣ Fetch LMS Holidays (ONCE)
        // -------------------------------
        List<HolidayDTO> lmsHolidays = fetchLmsHolidays(month, year, authHeader);

        // -------------------------------
        // 3️⃣ Generate Weekend Holidays (ONCE)
        // -------------------------------
        List<HolidayDTO> weekendHolidays = generateWeekendHolidays(month, year);

        // -------------------------------
        // 4️⃣ Fetch ALL LEAVES of all employees (ONCE)
        // -------------------------------
        List<LeaveDTO> allLeaves = leaveDirectoryService.fetchLeaves(year, month, authHeader);

        // -------------------------------
        // 5️⃣ Fetch ALL WEEKS OF MONTH
        // -------------------------------
        List<WeekInfo> weekInfos = weekInfoRepo.findByMonthAndYear(month, year);

        // -------------------------------
        // 6️⃣ PROCESS EACH USER
        // -------------------------------
        for (Long userId : users.keySet()) {

            List<HolidayDTO> userHolidayCalendar =
                    buildUserMonthlyCalendar(userId, month, year,
                            lmsHolidays, weekendHolidays, allLeaves);

            Map<LocalDate, List<HolidayDTO>> holidayMap = userHolidayCalendar.stream()
                    .collect(Collectors.groupingBy(
                            HolidayDTO::getHolidayDate,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            for (WeekInfo week : weekInfos) {

                boolean exists = weeklyReviewRepo.findByUserIdAndWeekInfo_Id(userId, week.getId()).isPresent();
                if (exists) continue;

                List<LocalDate> weekDates = week.getStartDate()
                        .datesUntil(week.getEndDate().plusDays(1))
                        .collect(Collectors.toList());

                if (!isFullHolidayWeek(weekDates, holidayMap)) continue;

                autoGenerateTimesheets(userId, week, weekDates);
                createWeeklyReview(userId, week);
            }
        }
    }

    // ------------------------- 
    // Build User Calendar 
    // -------------------------
    private List<HolidayDTO> buildUserMonthlyCalendar(
            Long userId,
            int month,
            int year,
            List<HolidayDTO> lmsHolidays,
            List<HolidayDTO> weekendHolidays,
            List<LeaveDTO> allLeaves
    ) {

        // Fetch exclusions
        List<HolidayExcludeUsers> excluded = holidayExcludeUsersRepo.findByUserId(userId);
        Set<LocalDate> excludedDates = excluded.stream()
                .map(HolidayExcludeUsers::getHolidayDate)
                .collect(Collectors.toSet());

        // Filter leaves for user
        List<HolidayDTO> userLeaves = convertLeavesToHolidayDTO(allLeaves, userId, month, year);

        // Combine all
        List<HolidayDTO> combined = new ArrayList<>();
        combined.addAll(lmsHolidays);
        combined.addAll(weekendHolidays);
        combined.addAll(userLeaves);

        // Apply user exclusions + merge duplicates
        Map<LocalDate, List<HolidayDTO>> grouped =
                combined.stream()
                        .collect(Collectors.groupingBy(
                                HolidayDTO::getHolidayDate,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        List<HolidayDTO> finalList = new ArrayList<>();

        for (Map.Entry<LocalDate, List<HolidayDTO>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();
            List<HolidayDTO> list = entry.getValue();

            HolidayDTO merged = mergeHolidayEntries(list);

            // Apply user exclusions
            boolean submitTimesheetOverride = excludedDates.contains(date);
            if (submitTimesheetOverride) {
                merged.setSubmitTimesheet(true);
                merged.setTimeSheetReviews("Allowed to Submit on Holiday");
            }

            finalList.add(merged);
        }

        return finalList.stream()
                .sorted(Comparator.comparing(HolidayDTO::getHolidayDate))
                .toList();
    }

    // -------------------------
    // Merge duplicates correctly
    // -------------------------
    private HolidayDTO mergeHolidayEntries(List<HolidayDTO> list) {

        HolidayDTO base = list.get(0);

        boolean submit = list.stream().anyMatch(HolidayDTO::isSubmitTimesheet);

        base.setSubmitTimesheet(submit);

        return base;
    }

    // -------------------------
    // Convert LEAVES → HolidayDTO
    // -------------------------
    private List<HolidayDTO> convertLeavesToHolidayDTO(List<LeaveDTO> allLeaves, Long userId, int month, int year) {

        return allLeaves.stream()
                .filter(l -> l.getEmployeeId().equals(userId))
                .flatMap(l -> {
                    LocalDate start = l.getStartDate();
                    LocalDate end = l.getEndDate();

                    LocalDate mStart = LocalDate.of(year, month, 1);
                    LocalDate mEnd = mStart.withDayOfMonth(mStart.lengthOfMonth());

                    LocalDate s = start.isBefore(mStart) ? mStart : start;
                    LocalDate e = end.isAfter(mEnd) ? mEnd : end;

                    List<HolidayDTO> list = new ArrayList<>();

                    for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                        if (d.getDayOfWeek() == DayOfWeek.SATURDAY ||
                                d.getDayOfWeek() == DayOfWeek.SUNDAY)
                            continue;

                        HolidayDTO dto = new HolidayDTO();
                        dto.setHolidayId(0L);
                        dto.setHolidayName("Leave Day");
                        dto.setHolidayDate(d);
                        dto.setHolidayDescription(l.getReason());
                        dto.setType("LEAVE");
                        dto.setCountry("India");
                        dto.setState("All States");
                        dto.setYear(year);
                        dto.setLeave(true);
                        dto.setSubmitTimesheet(false);
                        dto.setTimeSheetReviews(
                                l.getStatus().equalsIgnoreCase("APPROVED") ? "Leave Approved" : "Leave Pending"
                        );

                        list.add(dto);
                    }

                    return list.stream();
                })
                .toList();
    }

    // -------------------------
    // Weekend generator
    // -------------------------
    private List<HolidayDTO> generateWeekendHolidays(int month, int year) {

        List<HolidayDTO> weekends = new ArrayList<>();

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {

            if (d.getDayOfWeek() == DayOfWeek.SATURDAY ||
                d.getDayOfWeek() == DayOfWeek.SUNDAY) {

                HolidayDTO dto = new HolidayDTO();
                dto.setHolidayId(0L);
                dto.setHolidayName(d.getDayOfWeek() == DayOfWeek.SATURDAY ? "Saturday" : "Sunday");
                dto.setHolidayDate(d);
                dto.setHolidayDescription("Casual Monthly Weekend");
                dto.setType("WEEKEND");
                dto.setCountry("India");
                dto.setState("All States");
                dto.setSubmitTimesheet(false);
                dto.setTimeSheetReviews("Weekend Holiday");

                weekends.add(dto);
            }
        }

        return weekends;
    }

    // -------------------------
    // LMS Holidays (ONCE)
    // -------------------------
    private List<HolidayDTO> fetchLmsHolidays(int month, int year, String authHeader) {

        try {
            String url = String.format("%s/api/holidays/month/%d", lmsBaseUrl, month);

            ResponseEntity<List<HolidayDTO>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(Map.of("Authorization", authHeader)),
                            new ParameterizedTypeReference<List<HolidayDTO>>() {}
                    );

            return response.getBody() != null ? response.getBody() : List.of();

        } catch (Exception e) {
            System.err.println("⚠ Failed LMS holidays: " + e.getMessage());
            return List.of();
        }
    }

    // -------------------------
    // Check Full Holiday Week
    // -------------------------
    private boolean isFullHolidayWeek(
            List<LocalDate> weekDates,
            Map<LocalDate, List<HolidayDTO>> holidayMap
    ) {
        for (LocalDate d : weekDates) {

            List<HolidayDTO> list = holidayMap.get(d);

            if (list == null || list.isEmpty())
                return false;

            boolean requiresSubmission = list.stream()
                    .anyMatch(HolidayDTO::isSubmitTimesheet);

            if (requiresSubmission)
                return false;
        }
        return true;
    }

    // -------------------------
    // Auto-generate timesheets
    // -------------------------
    private void autoGenerateTimesheets(Long userId, WeekInfo week, List<LocalDate> dates) {

        LocalDateTime now = LocalDateTime.now();

        for (LocalDate d : dates) {

            if (timeSheetRepo.existsByUserIdAndWorkDate(userId, d))
                continue;

            boolean weekend = (d.getDayOfWeek() == DayOfWeek.SATURDAY ||
                               d.getDayOfWeek() == DayOfWeek.SUNDAY);

            BigDecimal hours = weekend ? BigDecimal.ZERO : BigDecimal.valueOf(8);

            TimeSheet ts = new TimeSheet();
            ts.setUserId(userId);
            ts.setWorkDate(d);
            ts.setWeekInfo(week);
            ts.setHoursWorked(hours);
            ts.setAutoGenerated(true);
            ts.setStatus(TimeSheet.Status.APPROVED);
            ts.setCreatedAt(now);
            ts.setUpdatedAt(now);

            timeSheetRepo.save(ts);
        }
    }

    // -------------------------
    // Create Weekly Review
    // -------------------------
    private void createWeeklyReview(Long userId, WeekInfo week) {

        WeeklyTimeSheetReview r = new WeeklyTimeSheetReview();
        r.setUserId(userId);
        r.setWeekInfo(week);
        r.setStatus(WeeklyTimeSheetReview.Status.APPROVED);
        r.setSubmittedAt(LocalDateTime.now());
        r.setReviewedAt(LocalDateTime.now());

        weeklyReviewRepo.save(r);
    }
}
