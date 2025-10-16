package com.intranet.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.WeeklyTimeSheetReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/weeklyReview")
@RequiredArgsConstructor
public class WeeklyTimeSheetReviewController {

    private final WeeklyTimeSheetReviewService reviewService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitWeeklyTimesheets(
            @CurrentUser UserDTO user,
            @RequestBody List<Long> timesheetIds) {
        try {
            String message = reviewService.submitWeeklyTimeSheets(user.getId(), timesheetIds);
            return ResponseEntity.ok().body(message);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Failed to submit timesheets: " + ex.getMessage());
        }
    }
}
