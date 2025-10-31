package com.intranet.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetReview;
import com.intranet.entity.WeekInfo;
import com.intranet.entity.WeeklyTimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.TimeSheetReviewRepo;
import com.intranet.repository.WeeklyTimeSheetReviewRepo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeeklyTimeSheetReviewService {

    private final TimeSheetRepo timeSheetRepo;
    private final WeeklyTimeSheetReviewRepo weeklyReviewRepo;
    private final TimeSheetReviewRepo timeSheetReviewRepo;

    @Value("${tms.api.base-url}")
    private String tmsBaseUrl;
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


    // @Transactional
    // public String submitWeeklyTimeSheets(Long userId, List<Long> timeSheetIds) {
    //     if (userId == null || timeSheetIds == null || timeSheetIds.isEmpty()) {
    //         throw new IllegalArgumentException("User ID and TimeSheet IDs are required.");
    //     }

    //     // Fetch timesheets
    //     List<TimeSheet> timeSheets = timeSheetRepo.findAllById(timeSheetIds);
    //     if (timeSheets.isEmpty()) {
    //         throw new IllegalArgumentException("No timesheets found for provided IDs.");
    //     }

    //     // Check same user & week
    //     TimeSheet firstTs = timeSheets.get(0);
    //     WeekInfo commonWeek = firstTs.getWeekInfo();
    //     Long commonUser = firstTs.getUserId();

    //     boolean valid = timeSheets.stream()
    //             .allMatch(ts -> ts.getWeekInfo().getId().equals(commonWeek.getId())
    //                     && ts.getUserId().equals(commonUser));

    //     if (!valid) {
    //         throw new IllegalArgumentException("All timesheets must belong to the same user and week.");
    //     }

    //     if (!commonUser.equals(userId)) {
    //         throw new IllegalArgumentException("User not authorized to submit these timesheets.");
    //     }

    //         // Check if a weekly review already exists for this user & week
    //     WeeklyTimeSheetReview review = weeklyReviewRepo
    //             .findByUserIdAndWeekInfo_Id(userId, commonWeek.getId())
    //             .orElseGet(WeeklyTimeSheetReview::new);
                
    //             // ✅ Allow submission only if new or status is SUBMITTED
    //     if (review.getStatus() != null 
    //             && review.getStatus() != WeeklyTimeSheetReview.Status.SUBMITTED) {
    //         throw new IllegalStateException(
    //             "Cannot submit timesheets for this week. Weekly review is already " + review.getStatus()
    //         );
    //     }

    //     // Update all timesheets from DRAFT → SUBMITTED
    //     timeSheets.forEach(ts -> {
    //         if (ts.getStatus() == TimeSheet.Status.DRAFT) {
    //             ts.setStatus(TimeSheet.Status.SUBMITTED);
    //             ts.setUpdatedAt(LocalDateTime.now());
    //         }
    //     });

    //     timeSheetRepo.saveAll(timeSheets);
        
    //     // Set/update fields
    //     review.setWeekInfo(commonWeek);
    //     review.setStatus(WeeklyTimeSheetReview.Status.SUBMITTED);
    //     review.setSubmittedAt(LocalDateTime.now());
    //     review.setReviewedAt(LocalDateTime.now());
    //     review.setUserId(userId);

    //     weeklyReviewRepo.save(review);

    //     String monthName = commonWeek.getStartDate()
    //     .getMonth()
    //     .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);

    //     return String.format(
    //         "Timesheets submitted successfully for week %d of %s %d",
    //         commonWeek.getWeekNo(),
    //         monthName,
    //         commonWeek.getYear()
    //     );
    // }
        @Transactional
        public String submitWeeklyTimeSheets(Long userId, List<Long> timeSheetIds) {
        if (userId == null || timeSheetIds == null || timeSheetIds.isEmpty()) {
            throw new IllegalArgumentException("User ID and TimeSheet IDs are required.");
        }

        // Fetch timesheets
        List<TimeSheet> timeSheets = timeSheetRepo.findAllById(timeSheetIds);
        if (timeSheets.isEmpty()) {
            throw new IllegalArgumentException("No timesheets found for provided IDs.");
        }

        // Validate same user & week
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

        // ✅ Step 1: Calculate total hours worked
        BigDecimal totalWorked = timeSheets.stream()
                .map(TimeSheet::getHoursWorked)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ✅ Step 2: Fetch holidays for the current month
        HttpEntity<Void> entity = buildEntityWithAuth(); // reuse from your other service
        String url = String.format("%s/api/holidays/currentMonth", tmsBaseUrl);

        List<Map<String, Object>> holidays = List.of();
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
            holidays = response.getBody();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to fetch holidays for current month.");
        }

        // ✅ Step 3: Filter holidays that fall within this week and are NOT submitTimesheet=true
        LocalDate start = commonWeek.getStartDate();
        LocalDate end = commonWeek.getEndDate();

        long holidayCount = holidays.stream()
                .filter(h -> {
                    LocalDate date = LocalDate.parse(h.get("holidayDate").toString());
                    boolean submitAllowed = Boolean.TRUE.equals(h.get("submitTimesheet"));
                    return !submitAllowed && (date.isEqual(start) || date.isEqual(end) ||
                            (date.isAfter(start) && date.isBefore(end)));
                })
                .count();

        // ✅ Step 4: Compute required hours (40 - (8 * holidays))
        BigDecimal requiredHours = BigDecimal.valueOf(40 - (holidayCount * 8));

        // // ✅ Step 5: Validation check
        // if (totalWorked.compareTo(requiredHours) < 0) {
        //     throw new IllegalArgumentException(String.format(
        //         "Weekly total hours %.2f are less than required minimum %.2f hours for  %d holidays.",
        //         totalWorked, requiredHours, holidayCount
        //     ));
        // }

            // ✅ Step 6: Check existing weekly review
            Optional<WeeklyTimeSheetReview> existingReviewOpt =
                    weeklyReviewRepo.findByUserIdAndWeekInfo_Id(userId, commonWeek.getId());

            WeeklyTimeSheetReview review;

            if (existingReviewOpt.isPresent()) {
                // 🔹 Update existing record
                review = existingReviewOpt.get();

                // Prevent resubmission if already submitted/approved
                if (
                    review.getStatus() == WeeklyTimeSheetReview.Status.APPROVED) {
                    throw new IllegalStateException(
                        "Weekly review already " + review.getStatus() + " for this week."
                    );
                }

                review.setStatus(WeeklyTimeSheetReview.Status.SUBMITTED);
                review.setReviewedAt(LocalDateTime.now());

            } else {
                // 🔹 Create new record
                review = new WeeklyTimeSheetReview();
                review.setWeekInfo(commonWeek);
                review.setUserId(userId);
                review.setStatus(WeeklyTimeSheetReview.Status.SUBMITTED);
                review.setSubmittedAt(LocalDateTime.now());
                review.setReviewedAt(LocalDateTime.now());
            }

            // ✅ Step 7: Update all timesheets from DRAFT → SUBMITTED
            timeSheets.forEach(ts -> {
                ts.setStatus(TimeSheet.Status.SUBMITTED);
                ts.setUpdatedAt(LocalDateTime.now());
            });
            timeSheetRepo.saveAll(timeSheets);

            // ✅ Step 8: Save the weekly review
            weeklyReviewRepo.save(review);

            // ✅ Step 5: Update existing TimeSheetReview records → PENDING
            List<TimeSheetReview> existingReviews = timeSheetReviewRepo.findByTimeSheet_IdIn(timeSheetIds);

            if (existingReviews != null && !existingReviews.isEmpty()) {
                for (TimeSheetReview r : existingReviews) {
                    r.setStatus(TimeSheetReview.Status.SUBMITTED);
                    r.setReviewedAt(LocalDateTime.now());
                }

                timeSheetReviewRepo.saveAll(existingReviews);
            }

        String monthName = commonWeek.getStartDate()
                .getMonth()
                .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);

        return String.format(
                "Timesheets submitted successfully for week %d of %s %d.",
                commonWeek.getWeekNo(),
                monthName,
                commonWeek.getYear()
        );
        }

}
