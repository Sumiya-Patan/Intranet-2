package com.intranet.service;

import com.intranet.dto.TimeSheetBulkReviewRequestDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetReview;
import com.intranet.entity.WeekInfo;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.TimeSheetReviewRepo;
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

        // ✅ Reflect the review status directly on the timesheet
        ts.setStatus(TimeSheet.Status.valueOf(status));
        timeSheetRepo.save(ts);
        
    }

        return String.format("%d timesheets %s successfully.", 
                sheets.size(), dto.getStatus().toUpperCase());
    }
}
