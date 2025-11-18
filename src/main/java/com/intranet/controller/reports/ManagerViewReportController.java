package com.intranet.controller.reports;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.report.ManagerMonthlyReportService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class ManagerViewReportController {

    private final ManagerMonthlyReportService managerMonthlyReportService;

    @GetMapping("/managerMonthly")
    public ResponseEntity<?> managerMonthlyReport(
            @CurrentUser UserDTO currentUser,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            HttpServletRequest req
    ) {

        String token = req.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Authorization token is missing");
        }

        try {
            // Default to current month/year if not provided
            LocalDate today = LocalDate.now();
            int selectedMonth = (month != null && month >= 1 && month <= 12) ? month : today.getMonthValue();
            int selectedYear  = (year != null && year > 2000) ? year : today.getYear();

            // Build correct date range
            LocalDate startDate = LocalDate.of(selectedYear, selectedMonth, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

            // Generate report
            Map<String, Object> report =
                    managerMonthlyReportService.generateManagerMonthlyReport(
                            currentUser.getId(),
                            startDate,
                            endDate,
                            token
                    );

            return ResponseEntity.ok(report);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
