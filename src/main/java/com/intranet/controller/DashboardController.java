package com.intranet.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.UserDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.security.CurrentUser;
import com.intranet.service.DashboardService;
import com.intranet.service.TimeUtil;

import io.swagger.v3.oas.annotations.Operation;


@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*",allowedHeaders = "*")
public class DashboardController {

    @Autowired
    private TimeSheetRepo timeSheetRepo;
    @Autowired
    private DashboardService dashboardService;

    /**
     * Get total hours entered by a user in the current month
     */
    @GetMapping("/total_hours")
    @Operation(summary = "Get total hours entered by a user in the current month")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<?> getTotalHoursCurrentMonth(
        @CurrentUser UserDTO user
    ) 
    {
        if (user.getId() == null) {
            return ResponseEntity.badRequest().body("User ID cannot be null");
        }

        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        // Fetch timesheets for user in current month
        List<TimeSheet> timesheets = timeSheetRepo.findByUserIdAndWorkDateBetween(user.getId(), startOfMonth, endOfMonth);
        if (timesheets.isEmpty()) {
            return ResponseEntity.ok("No timesheets found for current month");
        }

        // Collect all hoursWorked values
        List<BigDecimal> hoursList = timesheets.stream()
                .map(TimeSheet::getHoursWorked)
                .collect(Collectors.toList());

        // Sum hours using TimeUtil
        BigDecimal totalHours = TimeUtil.sumHours(hoursList);

        return ResponseEntity.ok(Map.of("totalHours", totalHours.toPlainString()));
    }
    

    @GetMapping("/summary")
    @Operation(summary = "Get summary of the dashboard")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<?> getSummary(
        @CurrentUser UserDTO user
    ) {
        return ResponseEntity.ok(dashboardService.getDashboardSummary(user.getId(), LocalDate.now().withDayOfMonth(1), LocalDate.now().with(TemporalAdjusters.lastDayOfMonth())));
    }

    @GetMapping("/summary/lastMonth")
    @Operation(summary = "Get summary for the last 1 month window")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<?> getLastMonthSummary(
            @CurrentUser UserDTO user
    ) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(1);   // 10 Oct
        LocalDate endDate = today;                   // 10 Nov

        return ResponseEntity.ok(
                dashboardService.getDashboardSummary(
                        user.getId(),
                        startDate,
                        endDate
                )
        );
    }

    @GetMapping("/summary/last3Months")
    @Operation(summary = "Get summary for the last 3 months window")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<?> getLast3MonthsSummary(
            @CurrentUser UserDTO user
    ) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(3);  // 10 Aug
        LocalDate endDate = today;                  // 10 Nov

        return ResponseEntity.ok(
                dashboardService.getDashboardSummary(
                        user.getId(),
                        startDate,
                        endDate
                )
        );
    }

}
