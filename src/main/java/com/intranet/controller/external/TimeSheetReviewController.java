package com.intranet.controller.external;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.external.ManagerInfoDTO;
import com.intranet.dto.external.TimeSheetReviewRequest;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.entity.TimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.TimeSheetReviewRepo;
import com.intranet.service.external.ExternalProjectApiService;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
// @NoArgsConstructor
// @AllArgsConstructor
public class TimeSheetReviewController {

    @Autowired
    private final TimeSheetRepo timeSheetRepository;

    @Autowired
    private final TimeSheetReviewRepo reviewRepository;

    @Autowired
    private final ExternalProjectApiService externalProjectApiService;

    @PutMapping("/review/{managerId}/{status}")
    public ResponseEntity<String> reviewTimesheet(
            @PathVariable Long managerId,
            @PathVariable String status,
            @RequestBody TimeSheetReviewRequest request
    ) {
        TimeSheet timeSheet = timeSheetRepository.findById(request.getTimesheetId())
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

        // Get unique projectIds from the timesheet
        Set<Long> projectIds = timeSheet.getEntries()
                                        .stream()
                                        .map(TimeSheetEntry::getProjectId)
                                        .collect(Collectors.toSet());

        // Check if manager is assigned to at least one project
        boolean managerIsAssigned = projectIds.stream().anyMatch(projectId -> {
            List<ManagerInfoDTO> managers = externalProjectApiService.getManagersForProject(projectId);
            return managers.stream().anyMatch(m -> m.getManagerId().equals(managerId));
        });

        if (!managerIsAssigned) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body("Manager is not assigned to this timesheet's projects.");
        }

        // Update the status
        if (!status.equalsIgnoreCase("APPROVED") && !status.equalsIgnoreCase("REJECTED")) {
            return ResponseEntity.badRequest().body("Invalid status. Must be APPROVED or REJECTED");
        }

        timeSheet.setStatus(status.toUpperCase());
        timeSheet.setUpdatedAt(LocalDateTime.now());

        // Save the updated timesheet
        timeSheetRepository.save(timeSheet);

        // Add a review entry
        TimeSheetReview review = new TimeSheetReview();
        
        review.setTimeSheet(timeSheet);
        review.setManagerId(managerId);
        review.setAction(status.toUpperCase());
        review.setComment(request.getComment());
        review.setReviewedAt(LocalDateTime.now());

        reviewRepository.save(review);

        return ResponseEntity.ok("Timesheet status updated and review saved successfully.");
    }
}
