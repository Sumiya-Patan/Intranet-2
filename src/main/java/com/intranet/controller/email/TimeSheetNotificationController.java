package com.intranet.controller.email;

import com.intranet.dto.email.TimeSheetSummaryEmailDTO;
import com.intranet.service.email.TimeSheetNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timesheet")
@Tag(name = "Timesheet Notifications", description = "APIs for sending timesheet summary or status emails")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TimeSheetNotificationController {

    private final TimeSheetNotificationService timeSheetNotificationService;

    /**
     * Endpoint: POST /api/emails/timesheet-summary
     * Accepts a JSON array of timesheet summaries and sends professional emails to each user.
     */
    @Operation(summary = "Send timesheet summary/status emails to users")
    @PostMapping("/response")
    public ResponseEntity<?> sendTimeSheetSummaryEmails(
            @RequestBody List<TimeSheetSummaryEmailDTO> summaries
    ) {
        if (summaries == null || summaries.isEmpty()) {
            return ResponseEntity.badRequest().body("No email data provided.");
        }

        try {
            timeSheetNotificationService.sendTimeSheetSummaryEmails(summaries);
            return ResponseEntity.ok("✅ Emails have been sent successfully to " + summaries.size() + " recipients.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("❌ Failed to send emails: " + e.getMessage());
        }
    }
}
