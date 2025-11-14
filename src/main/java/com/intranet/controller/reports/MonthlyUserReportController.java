package com.intranet.controller.reports;

import com.intranet.dto.MonthlyUserReportDTO;
import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.MonthlyUserReportService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class MonthlyUserReportController {

    private final MonthlyUserReportService reportService;

    @GetMapping("/user_monthly")
    public ResponseEntity<?> getMonthlyReport(
            @CurrentUser UserDTO currentUser,
            @RequestParam int month,
            @RequestParam int year) {
        try {
        MonthlyUserReportDTO report = reportService.getMonthlyUserReport(currentUser.getId(), month, year);
        return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating report: " + e.getMessage());
        }
    }
}
