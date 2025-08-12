package com.intranet.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.external.ManagerInfoDTO;
import com.intranet.dto.external.TimeSheetReviewRequest;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.entity.TimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.TimeSheetReviewRepo;
import com.intranet.service.external.ExternalProjectApiService;

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

    @PutMapping("/review/{managerId}")
    public ResponseEntity<String> reviewTimesheet(
            @PathVariable Long managerId,
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
        review.setManagerId(managerId);
        review.setAction(status);
        review.setComment(request.getComment());
        review.setReviewedAt(LocalDateTime.now());

        reviewRepository.save(review);

        return ResponseEntity.ok("Timesheet status updated and review saved successfully.");
    }
}
