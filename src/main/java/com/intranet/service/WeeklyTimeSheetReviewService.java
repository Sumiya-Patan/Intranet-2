package com.intranet.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intranet.entity.TimeSheet;
import com.intranet.entity.WeekInfo;
import com.intranet.entity.WeeklyTimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.WeeklyTimeSheetReviewRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeeklyTimeSheetReviewService {

    private final TimeSheetRepo timeSheetRepo;
    private final WeeklyTimeSheetReviewRepo weeklyReviewRepo;

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

        // Check same user & week
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

        // Update all timesheets from DRAFT → SUBMITTED
        timeSheets.forEach(ts -> {
            if (ts.getStatus() == TimeSheet.Status.DRAFT) {
                ts.setStatus(TimeSheet.Status.SUBMITTED);
                ts.setUpdatedAt(LocalDateTime.now());
            }
        });

        timeSheetRepo.saveAll(timeSheets);

            // Check if a weekly review already exists for this user & week
        WeeklyTimeSheetReview review = weeklyReviewRepo
                .findByUserIdAndWeekInfo_Id(userId, commonWeek.getId())
                .orElseGet(WeeklyTimeSheetReview::new);
                
        if (review.getStatus()!= null && review.getStatus() != WeeklyTimeSheetReview.Status.APPROVED) {
            throw new IllegalStateException("Weekly already approved.");
        }
        
        // Set/update fields
        review.setWeekInfo(commonWeek);
        review.setStatus(WeeklyTimeSheetReview.Status.SUBMITTED);
        review.setSubmittedAt(LocalDateTime.now());
        review.setReviewedAt(LocalDateTime.now());
        review.setUserId(userId);

        weeklyReviewRepo.save(review);

        String monthName = commonWeek.getStartDate()
        .getMonth()
        .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);

        return String.format(
            "Timesheets submitted successfully for week %d of %s %d",
            commonWeek.getWeekNo(),
            monthName,
            commonWeek.getYear()
        );
    }
}
