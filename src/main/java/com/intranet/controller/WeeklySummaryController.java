package com.intranet.controller;

import com.intranet.dto.UserDTO;
import com.intranet.dto.WeeklySummaryDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.WeeklySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class WeeklySummaryController {

    private final WeeklySummaryService weeklySummaryService;

    /**
     * Get weekly summary of a user for the current month
     *
     * @param userId the ID of the user
     * @return weekly summary DTO
     */
    @GetMapping("/weekly-summary")
    public ResponseEntity<WeeklySummaryDTO> getWeeklySummary(@CurrentUser UserDTO user) {
        WeeklySummaryDTO summary = weeklySummaryService.getWeeklySummary(user.getId());
        return ResponseEntity.ok(summary);
    }
}
