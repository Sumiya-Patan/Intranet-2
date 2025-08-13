package com.intranet.controller;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.UserDTO;
import com.intranet.dto.external.TimeSheetReviewRequest;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.TimeSheetReviewRepo;
import com.intranet.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class TimeSheetReviewController {

    @Autowired
    private final TimeSheetRepo timeSheetRepository;

    @Autowired
    private final TimeSheetReviewRepo reviewRepository;


    @Operation(summary = "Review a timesheet by manager")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('HR')")
    @PutMapping("/review")
    public ResponseEntity<String> reviewTimesheet(
            @CurrentUser UserDTO user,
            @RequestParam String status,
            @RequestBody TimeSheetReviewRequest request
    ) {
        TimeSheet timeSheet = timeSheetRepository.findById(request.getTimesheetId())
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));
        
        // Update the status
        if (!status.equalsIgnoreCase("Approved") && !status.equalsIgnoreCase("Rejected")) {
            return ResponseEntity.badRequest().body("Invalid status. Must be APPROVED or REJECTED");
        }

        // get comment from request if status is REJECTED
        if (status.equalsIgnoreCase("REJECTED") && request.getComment() == null) {
            return ResponseEntity.badRequest().body("Comment is required for rejected timesheets.");
        }

        timeSheet.setStatus(status);
        timeSheet.setUpdatedAt(LocalDateTime.now());

        // Save the updated timesheet
        timeSheetRepository.save(timeSheet);

        // Add a review entry
        TimeSheetReview review = new TimeSheetReview();
        
        review.setTimeSheet(timeSheet);
        review.setManagerId(user.getId());
        review.setAction(status);
        review.setComment(request.getComment());
        review.setReviewedAt(LocalDateTime.now());

        reviewRepository.save(review);

        return ResponseEntity.ok("Timesheet status updated and review saved successfully.");
    }
}
