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

import com.intranet.dto.BulkTimeSheetReviewRequest;
import com.intranet.dto.UserDTO;
import com.intranet.dto.external.TimeSheetReviewRequest;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.TimeSheetReviewRepo;
import com.intranet.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
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
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('HR')")
    @PreAuthorize("@endpointRoleService.hasAccess(#request.requestURI, #request.method, authentication)")
    @PutMapping("/review")
    public ResponseEntity<String> reviewTimesheet(
            @CurrentUser UserDTO user,
            @RequestParam String status,
            @RequestBody TimeSheetReviewRequest request1,
            HttpServletRequest request
    ) {
        TimeSheet timeSheet = timeSheetRepository.findById(request1.getTimesheetId())
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));
        
        // Update the status
        if (!status.equalsIgnoreCase("Approved") && !status.equalsIgnoreCase("Rejected")) {
            return ResponseEntity.badRequest().body("Invalid status. Must be APPROVED or REJECTED");
        }

        // get comment from request if status is REJECTED
        if (status.equalsIgnoreCase("REJECTED") && request1.getComment() == null) {
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
        review.setComment(request1.getComment());
        review.setReviewedAt(LocalDateTime.now());

        reviewRepository.save(review);

        return ResponseEntity.ok("Timesheet status updated and review saved successfully.");
    }

    @Operation(summary = "Bulk review timesheets by manager")
    @PreAuthorize("@endpointRoleService.hasAccess(#request.requestURI, #request.method, authentication)")
    @PutMapping("/review/bulk")
    public ResponseEntity<String> bulkReviewTimesheets(
            @CurrentUser UserDTO user,
            @RequestBody BulkTimeSheetReviewRequest bulkRequest,
            HttpServletRequest request
    ) {
        String status = bulkRequest.getStatus();

        if (!"Approved".equalsIgnoreCase(status) && !"Rejected".equalsIgnoreCase(status)) {
            return ResponseEntity.badRequest().body("Invalid status. Must be APPROVED or REJECTED.");
        }

        if ("Rejected".equalsIgnoreCase(status) && (bulkRequest.getComment() == null || bulkRequest.getComment().isBlank())) {
            return ResponseEntity.badRequest().body("Comment is required for rejected timesheets.");
        }

        // Fetch all timesheets by IDs
        var timeSheets = timeSheetRepository.findAllById(bulkRequest.getTimesheetIds());

        if (timeSheets.isEmpty()) {
            return ResponseEntity.badRequest().body("No valid timesheets found for given IDs.");
        }

        for (TimeSheet ts : timeSheets) {
            ts.setStatus(status);
            ts.setUpdatedAt(LocalDateTime.now());
            timeSheetRepository.save(ts);

            // Create review entry
            TimeSheetReview review = new TimeSheetReview();
            review.setTimeSheet(ts);
            review.setManagerId(user.getId());
            review.setAction(status);
            review.setComment(bulkRequest.getComment());
            review.setReviewedAt(LocalDateTime.now());

            reviewRepository.save(review);
        }

        return ResponseEntity.ok("Bulk timesheet review completed successfully.");
        }


}
