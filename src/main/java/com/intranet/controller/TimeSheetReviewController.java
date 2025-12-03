package com.intranet.controller;

import com.intranet.dto.TimeSheetBulkReviewRequestDTO;
import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.TimeSheetReviewService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/timesheets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*",allowedHeaders = "*")
public class TimeSheetReviewController {

    private final TimeSheetReviewService reviewService;

    @PostMapping("/review")
    @PreAuthorize("hasAuthority('APPROVE_TIMESHEET')")
    @Operation(summary = "Approve or reject multiple timesheets for a user by manager")
    public ResponseEntity<?> reviewMultipleTimesheets(
            @CurrentUser UserDTO manager,
            @RequestBody TimeSheetBulkReviewRequestDTO dto) {

        String message = reviewService.reviewMultipleTimesheets(manager.getId(), dto);
        return ResponseEntity.ok().body(
                java.util.Map.of("message", message, "status", dto.getStatus())
        );
    }


    @PostMapping("/review/internal")
    @PreAuthorize("hasAuthority('REVIEW_INTERNAL_TIMESHEET')")
    @Operation(summary = "Approve or reject multiple timesheets for a user by manager internal")
    public ResponseEntity<?> reviewMultipleTimesheetsInternal(
            @CurrentUser UserDTO manager,
            @RequestBody TimeSheetBulkReviewRequestDTO dto) {

        String message = reviewService.reviewInternalTimesheets(manager.getId(), dto);
        return ResponseEntity.ok().body(
                java.util.Map.of("message", message, "status", dto.getStatus())
        );
    }

    @PostMapping("/review/internal/bulk")
    @PreAuthorize("hasAuthority('REVIEW_INTERNAL_TIMESHEET')")
    @Operation(summary = "Approve or reject multiple timesheets for a user by manager internal")
    public ResponseEntity<?> reviewMultipleTimesheetsInternal(
            @CurrentUser UserDTO manager,
            @RequestBody List<TimeSheetBulkReviewRequestDTO> dto) {
        
        try{
          for(TimeSheetBulkReviewRequestDTO ts : dto) {
              reviewService.reviewInternalTimesheets(manager.getId(), ts);
          }
          return ResponseEntity.ok().body("Success");
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Failed to submit timesheets");
        }
    }
}
