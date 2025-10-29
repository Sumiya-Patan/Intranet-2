package com.intranet.controller;

import com.intranet.dto.UserDTO;
import com.intranet.dto.WeeklySummaryDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.WeeklySummaryService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timesheet")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WeeklySummaryController {

    private final WeeklySummaryService weeklySummaryService;

    /**
     * Get weekly summary of a user for the current month
     *
     * @param userId the ID of the user
     * @return weekly summary DTO
     */
    // @GetMapping("/history")
    // @Operation(summary = "Get weekly summary of a user for the current month")
    // public ResponseEntity<WeeklySummaryDTO> getWeeklySummary(@CurrentUser UserDTO user) {
    //     WeeklySummaryDTO summary = weeklySummaryService.getWeeklySummary(user.getId());
    //     return ResponseEntity.ok(summary);
    // }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') OR hasAuthority('APPROVE_TIMESHEET')")
    @Operation(summary = "Get weekly summary of a user for the current month")
    public ResponseEntity<?> getWeeklySummary2(@CurrentUser UserDTO user) {
        try{
        WeeklySummaryDTO summary = weeklySummaryService.getUserWeeklyTimeSheetHistory(user.getId());
        return ResponseEntity.ok(summary);
        }
        catch(Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
