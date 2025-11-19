package com.intranet.service.cornjobs.CompleteWeekHoliday;

import com.intranet.dto.HolidayDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.WeekInfo;
import com.intranet.entity.WeeklyTimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.WeekInfoRepo;
import com.intranet.repository.WeeklyTimeSheetReviewRepo;
import com.intranet.service.HolidayExcludeUsersService;
import com.intranet.service.email.ums_corn_job_token.UmsAuthService;
import com.intranet.util.cache.UserDirectoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final HolidayExcludeUsersService holidayExcludeUsersService;

    private final TimeSheetRepo timeSheetRepo;
    private final WeeklyTimeSheetReviewRepo weeklyReviewRepo;

    @Transactional
    public void processPreviousMonth() {

        LocalDate now = LocalDate.now();
        LocalDate prevMonthDate = now.minusMonths(1);
        int month = prevMonthDate.getMonthValue();
        int year = prevMonthDate.getYear();

        System.out.println("🔍 Processing previous month: " + month + "-" + year);

        // STEP 1 → Fetch UMS token
        String umsToken = umsAuthService.getUmsToken();
        String authHeader = "Bearer " + umsToken;

        // STEP 2 → Fetch all users from UMS
        Map<Long, Map<String, Object>> allUsers =
                userDirectoryService.fetchAllUsers(authHeader);

        System.out.println("👥 Total Users Found: " + allUsers.size());

        // STEP 3 → Fetch all weeks for this month
        List<WeekInfo> weekInfos = weekInfoRepo.findByMonthAndYear(month, year);

        for (Long userId : allUsers.keySet()) {

            for (WeekInfo weekInfo : weekInfos) {

                // CASE 5 — Already reviewed → Skip
                boolean exists = weeklyReviewRepo
                        .findByUserIdAndWeekInfo_Id(userId, weekInfo.getId())
                        .isPresent();

                if (exists) {
                    continue;
                }

                // STEP 4 → Fetch holidays for USER specifically
                List<HolidayDTO> holidays =
                        holidayExcludeUsersService.getUserHolidaysAndLeave(userId, month, year);

                // Build holidayMap
                Map<LocalDate, Map<String, Object>> holidayMap =
                        holidays.stream()
                                .filter(h -> h.getHolidayDate() != null)
                                .collect(Collectors.toMap(
                                        HolidayDTO::getHolidayDate,
                                        this::convertHolidayToMap,
                                        (a, b) -> a,
                                        LinkedHashMap::new
                                ));

                // Build weekly date list
                List<LocalDate> weekDates = weekInfo.getStartDate()
                        .datesUntil(weekInfo.getEndDate().plusDays(1))
                        .collect(Collectors.toList());

                // CASE 1 — Full Holiday Week?
                boolean fullHolidayWeek = isFullHolidayWeek(weekDates, holidayMap);

                if (!fullHolidayWeek) {
                    continue;
                }

                // Auto-generate timesheets
                autoGenerateTimesheets(userId, weekInfo, weekDates);

                // Auto-approve weekly review
                createApprovedReview(userId, weekInfo);
            }
        }
    }

    private boolean isFullHolidayWeek(List<LocalDate> weekDates,
                                      Map<LocalDate, Map<String, Object>> holidayMap) {

        for (LocalDate date : weekDates) {
            Map<String, Object> h = holidayMap.get(date);

            if (h == null) return false;

            Object submit = h.get("submitTimesheet");

            if (!(submit instanceof Boolean) || ((Boolean) submit)) {
                return false;
            }
        }
        return true;
    }

    private void autoGenerateTimesheets(Long userId,
                                        WeekInfo weekInfo,
                                        List<LocalDate> weekDates) {

        LocalDateTime now = LocalDateTime.now();

        for (LocalDate date : weekDates) {

            boolean weekend =
                    date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                            date.getDayOfWeek() == DayOfWeek.SUNDAY;

            BigDecimal hours = weekend ? BigDecimal.ZERO : BigDecimal.valueOf(8);

            TimeSheet ts = new TimeSheet();
            ts.setUserId(userId);
            ts.setWeekInfo(weekInfo);
            ts.setWorkDate(date);
            ts.setHoursWorked(hours);
            ts.setAutoGenerated(true);
            ts.setStatus(TimeSheet.Status.APPROVED);
            ts.setEntries(Collections.emptyList());
            ts.setCreatedAt(now);
            ts.setUpdatedAt(now);

            timeSheetRepo.save(ts);
        }
    }

    private void createApprovedReview(Long userId, WeekInfo weekInfo) {

        LocalDateTime now = LocalDateTime.now();

        WeeklyTimeSheetReview review = new WeeklyTimeSheetReview();
        review.setUserId(userId);
        review.setWeekInfo(weekInfo);
        review.setStatus(WeeklyTimeSheetReview.Status.APPROVED);
        review.setSubmittedAt(now);
        review.setReviewedAt(now);

        weeklyReviewRepo.save(review);
    }

    private Map<String, Object> convertHolidayToMap(HolidayDTO h) {

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("holidayId", h.getHolidayId());
        map.put("holidayName", h.getHolidayName());
        map.put("holidayDate", h.getHolidayDate().toString());
        map.put("submitTimesheet", h.isSubmitTimesheet());
        map.put("description", h.getHolidayDescription());

        return map;
    }
}
