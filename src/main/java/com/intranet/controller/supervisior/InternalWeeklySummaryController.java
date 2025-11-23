package com.intranet.controller.supervisior;

import com.intranet.dto.external.ManagerWeeklySummaryDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.supervisior.InternalWeeklySummaryService;
import com.intranet.dto.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class InternalWeeklySummaryController {

    private final InternalWeeklySummaryService internalWeeklyService;

    @GetMapping("/internal/summary")
    @Operation(summary = "Get weekly internal project summary for all users")
    @PreAuthorize("hasAuthority('REVIEW_INTERNAL_TIMESHEET')")
    public ResponseEntity<List<ManagerWeeklySummaryDTO>> getInternalWeeklySummary(
            @CurrentUser UserDTO user,
            HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        List<ManagerWeeklySummaryDTO> summary =
                internalWeeklyService.getInternalWeeklySummary(authHeader, startOfMonth, endOfMonth);

        return ResponseEntity.ok(summary);
    }
}
