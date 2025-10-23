package com.intranet.controller.external;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.UserDTO;
import com.intranet.dto.external.ManagerWeeklySummaryDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.external.ManagerWeeklySummaryService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*",allowedHeaders = "*")
public class ManagerWeeklySummaryController {

    private final ManagerWeeklySummaryService managerWeeklySummaryService;

    @GetMapping("/manager")
    @Operation(summary = "Get weekly submitted timesheets grouped by user for the manager")
    @PreAuthorize("hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<List<ManagerWeeklySummaryDTO>> getSubmittedWeeklySummary(
            @CurrentUser UserDTO user,
            HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        List<ManagerWeeklySummaryDTO> summary =
                managerWeeklySummaryService.getWeeklySubmittedTimesheetsByManager(user.getId(), authHeader);

        return ResponseEntity.ok(summary);
    }
}
