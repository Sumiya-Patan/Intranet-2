package com.intranet.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.intranet.dto.email.WeeklySubmissionEmailDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetReview;
import com.intranet.entity.WeekInfo;
import com.intranet.entity.WeeklyTimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.TimeSheetReviewRepo;
import com.intranet.repository.WeeklyTimeSheetReviewRepo;
import com.intranet.service.email.ManagerNotificationEmailService;
import com.intranet.util.cache.UserDirectoryService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeeklyTimeSheetReviewService {

        private final TimeSheetRepo timeSheetRepo;
        private final WeeklyTimeSheetReviewRepo weeklyReviewRepo;
        private final TimeSheetReviewRepo timeSheetReviewRepo;
        private final UserDirectoryService userDirectoryService;
        private final ManagerNotificationEmailService managerNotificationEmailService;

        @Value("${tms.api.base-url}")
        private String tmsBaseUrl;

        @Value("${pms.api.base-url}")
        private String pmsBaseUrl;

        private final RestTemplate restTemplate = new RestTemplate();

        private HttpEntity<Void> buildEntityWithAuth() {

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return (HttpEntity<Void>) HttpEntity.EMPTY;
        }

        HttpServletRequest request = attrs.getRequest();
        String authHeader = request.getHeader("Authorization");

        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set("Authorization", authHeader);
        }

        return new HttpEntity<>(headers);
        }

        @Transactional
    public String submitWeeklyTimeSheets(Long userId, List<Long> timeSheetIds) {
    if (userId == null || timeSheetIds == null || timeSheetIds.isEmpty()) {
        throw new IllegalArgumentException("User ID and TimeSheet IDs are required.");
    }

    // ✅ Step 1: Fetch and validate timesheets
    List<TimeSheet> timeSheets = timeSheetRepo.findAllById(timeSheetIds);
    if (timeSheets.isEmpty()) {
        throw new IllegalArgumentException("No timesheets found for provided IDs.");
    }

    TimeSheet firstTs = timeSheets.get(0);
    WeekInfo commonWeek = firstTs.getWeekInfo();
    Long commonUser = firstTs.getUserId();

    boolean valid = timeSheets.stream()
            .allMatch(ts -> ts.getWeekInfo().getId().equals(commonWeek.getId())
                    && ts.getUserId().equals(commonUser));

    if (!valid) {
        throw new IllegalArgumentException("All timesheets must belong to the same user and week.");
    }

    if (!commonUser.equals(userId)) {
        throw new IllegalArgumentException("User not authorized to submit these timesheets.");
    }

    // ✅ Step 2: Fetch holidays for the current month
    HttpEntity<Void> entity = buildEntityWithAuth();
    String url = String.format("%s/api/holidays/currentMonth", tmsBaseUrl);

    List<Map<String, Object>> holidays = List.of();
    try {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
        holidays = Optional.ofNullable(response.getBody()).orElse(List.of());
    } catch (Exception e) {
        System.err.println("⚠️ Unable to fetch holidays for current month: " + e.getMessage());
    }

    Map<LocalDate, Map<String, Object>> holidayMap = holidays.stream()
            .filter(h -> h.get("holidayDate") != null)
            .collect(Collectors.toMap(
                    h -> LocalDate.parse(h.get("holidayDate").toString()),
                    h -> h,
                    (a, b) -> a
            ));

    // ✅ Step 3: Identify all dates in the week range
    LocalDate start = commonWeek.getStartDate();
    LocalDate end = commonWeek.getEndDate();
    List<LocalDate> weekDates = start.datesUntil(end.plusDays(1)).toList();

    // ✅ Step 4: Determine required hours and required dates based on holidays and weekends
    Map<LocalDate, Integer> requiredHoursPerDate = new LinkedHashMap<>();

    for (LocalDate date : weekDates) {
        int defaultHours;
        boolean isWeekend = date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY ||
                            date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
        defaultHours = isWeekend ? 4 : 8;

        Map<String, Object> holiday = holidayMap.get(date);
        boolean submitAllowed = false;

        if (holiday != null && holiday.containsKey("submitTimesheet")) {
            submitAllowed = Boolean.TRUE.equals(holiday.get("submitTimesheet"));
        }

        if (holiday == null) {
            // Normal working/ weekend day, add default hours
            requiredHoursPerDate.put(date, defaultHours);
        } else if (submitAllowed) {
            // Holiday but allowed to submit
            requiredHoursPerDate.put(date, defaultHours);
        } else {
            // Holiday and NOT allowed to submit — skip that date entirely
        }
    }

    // ✅ Step 5: Validate if user submitted all required timesheets
    Set<LocalDate> submittedDates = timeSheets.stream()
            .map(TimeSheet::getWorkDate)
            .collect(Collectors.toSet());

    Set<LocalDate> requiredDates = requiredHoursPerDate.keySet();

    Set<LocalDate> missingDates = new HashSet<>(requiredDates);
    missingDates.removeAll(submittedDates);

    if (!missingDates.isEmpty()) {
        throw new IllegalArgumentException(
                "Missing timesheets for required dates: " +
                missingDates.stream().sorted().map(LocalDate::toString).collect(Collectors.joining(", "))
        );
    }

    // ✅ Step 6: Compute total required hours
    int totalRequiredHours = requiredHoursPerDate.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
    BigDecimal requiredHours = BigDecimal.valueOf(totalRequiredHours);

    // ✅ Step 7: Calculate total worked hours
    BigDecimal totalWorked = timeSheets.stream()
            .map(TimeSheet::getHoursWorked)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // ✅ Step 8: Validation check
    if (totalWorked.compareTo(requiredHours) < 0) {
        throw new IllegalArgumentException(String.format(
            "Weekly total hours %.2f are less than required minimum %.2f hours for week %d.",
            totalWorked, requiredHours, commonWeek.getWeekNo()
        ));
    }
            // ✅ Step 7.1: Identify and auto-generate timesheets for submitTimesheet = false dates
    List<LocalDate> weekDatesA = commonWeek.getStartDate().datesUntil(commonWeek.getEndDate().plusDays(1)).toList();

    List<LocalDate> autoGenerateDates = weekDatesA.stream()
            .filter(date -> {
                Map<String, Object> holiday = holidayMap.get(date);
                if (holiday == null) return false;
                Object flag = holiday.get("submitTimesheet");
                return flag instanceof Boolean && !(Boolean) flag;
            })
            .collect(Collectors.toList());

    System.out.println("🗓️ Auto-generation dates for week "
            + commonWeek.getWeekNo() + ": " + autoGenerateDates);

    for (LocalDate date : autoGenerateDates) {
        boolean alreadyExists = timeSheets.stream()
                .anyMatch(ts -> ts.getWorkDate().equals(date));

        if (!alreadyExists) {
            TimeSheet newSheet = new TimeSheet();
            newSheet.setUserId(userId);
            newSheet.setWeekInfo(commonWeek);
            newSheet.setWorkDate(date);
            newSheet.setHoursWorked(BigDecimal.valueOf(8)); // standard full day
            newSheet.setStatus(TimeSheet.Status.SUBMITTED);
            newSheet.setEntries(Collections.emptyList());
            newSheet.setCreatedAt(LocalDateTime.now());
            newSheet.setUpdatedAt(LocalDateTime.now());

            timeSheetRepo.save(newSheet);
            timeSheets.add(newSheet);
        }
    }

    // ✅ Step 9: Update or create WeeklyTimeSheetReview
    WeeklyTimeSheetReview review = weeklyReviewRepo
            .findByUserIdAndWeekInfo_Id(userId, commonWeek.getId())
            .orElseGet(() -> {
                WeeklyTimeSheetReview r = new WeeklyTimeSheetReview();
                r.setUserId(userId);
                r.setWeekInfo(commonWeek);
                r.setSubmittedAt(LocalDateTime.now());
                return r;
            });

    review.setStatus(WeeklyTimeSheetReview.Status.SUBMITTED);
    review.setReviewedAt(LocalDateTime.now());
    weeklyReviewRepo.save(review);

    // ✅ Step 10: Update all timesheets to SUBMITTED
    timeSheets.forEach(ts -> {
        ts.setStatus(TimeSheet.Status.SUBMITTED);
        ts.setUpdatedAt(LocalDateTime.now());
    });
    timeSheetRepo.saveAll(timeSheets);

    // ✅ Step 11: Update TimeSheetReview records → SUBMITTED
    List<TimeSheetReview> existingReviews = timeSheetReviewRepo.findByTimeSheet_IdIn(timeSheetIds);
    if (existingReviews != null && !existingReviews.isEmpty()) {
        existingReviews.forEach(r -> {
            r.setStatus(TimeSheetReview.Status.SUBMITTED);
            r.setReviewedAt(LocalDateTime.now());
        });
        timeSheetReviewRepo.saveAll(existingReviews);
    }

    // ✅ Step 12: Notify managers via email
    boolean result = notifyManagersOnWeeklySubmission(userId, commonWeek, totalWorked, timeSheets);

    String monthName = commonWeek.getStartDate()
            .getMonth()
            .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);

    return String.format(
            "Timesheets submitted successfully for week %d of %s %d. %s",
            commonWeek.getWeekNo(),
            monthName,
            commonWeek.getYear(),
            result ? "Notification sent to managers." : "Notification not sent to managers."
    );
}

        private boolean notifyManagersOnWeeklySubmission(Long userId, WeekInfo commonWeek, BigDecimal totalWorked, List<TimeSheet> timeSheets) {
            try {
                // ✅ Step 1: Extract Authorization from current request
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs == null) {
                    throw new IllegalStateException("No request context available for Authorization header.");
                }
                HttpServletRequest request = attrs.getRequest();
                String authHeader = request.getHeader("Authorization");

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", authHeader);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                // ✅ Step 2: Identify unique project IDs from timesheets
                List<Long> projectIds = timeSheets.stream()
                        .flatMap(ts -> ts.getEntries().stream())
                        .map(e -> e.getProjectId())
                        .distinct()
                        .toList();

                if (projectIds.isEmpty()) {
                    System.out.println("⚠️ No project IDs found in submitted timesheets. Skipping manager notification.");
                    return false;
                }

                // ✅ Step 3: Fetch project details from PMS
                String pmsUrl = String.format("%s/projects", pmsBaseUrl);
                ResponseEntity<List<Map<String, Object>>> pmsResponse = restTemplate.exchange(
                        pmsUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
                List<Map<String, Object>> allProjects = Optional.ofNullable(pmsResponse.getBody()).orElse(List.of());

                // ✅ Step 4: Filter only relevant projects by ID
                List<Map<String, Object>> userProjects = allProjects.stream()
                        .filter(p -> projectIds.contains(((Number) p.get("id")).longValue()))
                        .toList();

                if (userProjects.isEmpty()) {
                    System.out.println("⚠️ No matching projects found in PMS for submitted timesheets.");
                    return false;
                }

                // ✅ Step 5: Fetch user info from UMS (cached)
                Map<Long, Map<String, Object>> allUsers = userDirectoryService.fetchAllUsers(authHeader);
                Map<String, Object> userDetails = allUsers.getOrDefault(userId, Map.of(
                        "name", "Unknown User",
                        "email", "unknown@example.com"
                ));

                String userName = (String) userDetails.get("name");

                // ✅ Step 6: Prepare manager notification DTOs
                List<WeeklySubmissionEmailDTO> managerNotifications = userProjects.stream()
                        .map(p -> {
                            Map<String, Object> owner = (Map<String, Object>) p.get("owner");
                            if (owner == null) return null;
                            return new WeeklySubmissionEmailDTO(
                                    ((Number) owner.get("id")).longValue(),
                                    (String) owner.get("name"),
                                    (String) owner.get("email"),
                                    userId,
                                    userName,
                                    commonWeek.getStartDate(),
                                    commonWeek.getEndDate(),
                                    totalWorked
                            );
                        })
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                // ✅ Step 7: Send manager notification emails
                if (!managerNotifications.isEmpty()) {
                    managerNotificationEmailService.sendWeeklySubmissionEmails(managerNotifications);
                    System.out.println("✅ Weekly submission notifications sent to managers.");
                    return true;
                } else {
                    System.out.println("⚠️ No managers found for notification.");
                    return false;
                }

            } catch (Exception e) {
                System.err.println("⚠️ Failed to send manager notification emails: " + e.getMessage());
                return false;
            }
    }


}
