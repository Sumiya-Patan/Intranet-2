package com.intranet.service;

import com.intranet.dto.TimeSheetBulkReviewRequestDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetReview;
import com.intranet.entity.WeekInfo;
import com.intranet.entity.WeeklyTimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.TimeSheetReviewRepo;
import com.intranet.repository.WeekInfoRepo;
import com.intranet.repository.WeeklyTimeSheetReviewRepo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeSheetReviewService {

    private final TimeSheetRepo timeSheetRepo;
    private final TimeSheetReviewRepo reviewRepo;
    private final WeeklyTimeSheetReviewRepo weeklyReviewRepo;
    private final WeekInfoRepo weekInfoRepo;

    @Transactional
    public String reviewMultipleTimesheets(Long managerId, TimeSheetBulkReviewRequestDTO dto) {
        if (dto.getTimesheetIds() == null || dto.getTimesheetIds().isEmpty()) {
            throw new IllegalArgumentException("Timesheet IDs must be provided.");
        }

        List<TimeSheet> sheets = timeSheetRepo.findAllById(dto.getTimesheetIds());
        if (sheets.isEmpty()) {
            throw new IllegalArgumentException("No timesheets found for given IDs.");
        }

            // ✅ 1️⃣ Validate that all timesheets belong to the same week
        Long firstWeekId = sheets.get(0).getWeekInfo().getId();
        boolean allSameWeek = sheets.stream()
                .allMatch(ts -> ts.getWeekInfo().getId().equals(firstWeekId));

        if (!allSameWeek) {
            throw new IllegalArgumentException("All timesheets must belong to the same week.");
        }

        boolean allSubmitted = sheets.stream()
        .allMatch(ts -> ts.getStatus() == TimeSheet.Status.SUBMITTED);

        if (!allSubmitted) {
            throw new IllegalArgumentException("Only 'Submitted' timesheets can be reviewed.");
        }

                String status = dto.getStatus().toUpperCase();
        if (!status.equals("APPROVED") && !status.equals("REJECTED")) {
            throw new IllegalArgumentException("Invalid status. Must be APPROVED or REJECTED.");
        }

        if ("REJECTED".equalsIgnoreCase(dto.getStatus()) && 
        (dto.getComments() == null || dto.getComments().isBlank())) {
        throw new IllegalArgumentException("Comments are required when rejecting a timesheet.");
        }

     


        // ✅ Auto-detect weekInfo from first timesheet (assuming all belong to same week)
        WeekInfo weekInfo = sheets.get(0).getWeekInfo();
        Long userId = dto.getUserId();

        if (weekInfo.getEndDate().isBefore(LocalDate.now().minusDays(30))) {
        throw new IllegalArgumentException("Cannot review timesheets older than 30 days.");
        }

        for (TimeSheet ts : sheets) {
            TimeSheetReview review = reviewRepo
                    .findByTimeSheet_IdAndManagerId(ts.getId(), managerId)
                    .orElseGet(TimeSheetReview::new);

            review.setUserId(userId);
            review.setManagerId(managerId);
            review.setTimeSheet(ts);
            review.setWeekInfo(weekInfo);
            review.setStatus(TimeSheetReview.Status.valueOf(dto.getStatus().toUpperCase()));
            review.setComments(dto.getComments());
            review.setReviewedAt(LocalDateTime.now());

            reviewRepo.save(review);

        // ✅ Recalculate overall timesheet status based on all reviews
        List<TimeSheetReview> allReviews = reviewRepo.findByTimeSheet_Id(ts.getId());
        TimeSheet.Status overallStatus = calculateOverallStatus(allReviews);

        ts.setStatus(overallStatus);
        timeSheetRepo.save(ts);

        updateWeeklyTimeSheetReview(userId, firstWeekId);

        
    }

        return String.format("%d timesheets %s successfully.", 
                sheets.size(), dto.getStatus().toUpperCase());
    }

    private TimeSheet.Status calculateOverallStatus(List<TimeSheetReview> reviews) {
    if (reviews.isEmpty()) return TimeSheet.Status.SUBMITTED;

    boolean anyRejected = reviews.stream()
            .anyMatch(r -> r.getStatus() == TimeSheetReview.Status.REJECTED);

    boolean allApproved = !reviews.isEmpty() && reviews.stream()
            .allMatch(r -> r.getStatus() == TimeSheetReview.Status.APPROVED);

    boolean anyApproved = reviews.stream()
            .anyMatch(r -> r.getStatus() == TimeSheetReview.Status.APPROVED);

    if (anyRejected) return TimeSheet.Status.REJECTED;
    else if (allApproved) return TimeSheet.Status.APPROVED;
    else if (anyApproved) return TimeSheet.Status.PARTIALLY_APPROVED;
    else return TimeSheet.Status.SUBMITTED;
    
    }

    @Transactional
    public void updateWeeklyTimeSheetReview(Long userId, Long weekInfoId) {
    // Fetch all timesheets for this user and week
    List<TimeSheet> weekTimeSheets = timeSheetRepo.findByUserIdAndWeekInfo_Id(userId, weekInfoId);
    if (weekTimeSheets.isEmpty()) {
        
        return ;
    }

    // Determine the overall weekly status based on timesheet statuses
    boolean anyRejected = weekTimeSheets.stream()
            .anyMatch(ts -> ts.getStatus() == TimeSheet.Status.REJECTED);
    boolean allApproved = !weekTimeSheets.isEmpty() && weekTimeSheets.stream()
            .allMatch(ts -> ts.getStatus() == TimeSheet.Status.APPROVED);
    // boolean anyApproved = weekTimeSheets.stream()
    //         .anyMatch(ts -> ts.getStatus() == TimeSheet.Status.APPROVED || ts.getStatus() == TimeSheet.Status.PARTIALLY_APPROVED);

    WeeklyTimeSheetReview.Status weeklyStatus;
    if (anyRejected) {
        weeklyStatus = WeeklyTimeSheetReview.Status.REJECTED;
    } else if (allApproved) {
        weeklyStatus = WeeklyTimeSheetReview.Status.APPROVED;
    } 
     else {
        weeklyStatus = WeeklyTimeSheetReview.Status.SUBMITTED;
    }

    // Fetch existing weekly review or create a new one
    WeeklyTimeSheetReview weeklyReview = weeklyReviewRepo
            .findByUserIdAndWeekInfo_Id(userId, weekInfoId)
            .orElseGet(() -> {
                WeeklyTimeSheetReview r = new WeeklyTimeSheetReview();
                r.setUserId(userId);
                r.setWeekInfo(weekInfoRepo.findById(weekInfoId)
                        .orElseThrow(() -> new IllegalArgumentException("Week not found")));
                return r;
            });

    // Update status and timestamp
    weeklyReview.setStatus(weeklyStatus);
    weeklyReview.setReviewedAt(LocalDateTime.now());

    weeklyReviewRepo.save(weeklyReview);
}

}
