package com.intranet.controller.ManagerEmailReport;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.ManagerEmailReport.ManagerMonthlyReportPdfEmailService;
import com.intranet.service.ManagerEmailReport.ManagerReportDtoAdapter;
import com.intranet.service.report.ManagerMonthlyReportService;

import java.time.LocalDate;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ManagerMonthlyReportPdfController {

    private final ManagerMonthlyReportService managerMonthlyReportService;
    private final ManagerMonthlyReportPdfEmailService emailService;

    @Value("${ums.api.base-url}")
    private String umsBaseUrl;

    @GetMapping("/managerMonthlyPdf")
    @PreAuthorize("hasAuthority('APPROVE_TIMESHEET') or hasAuthority('VIEW_TIMESHEET')")
    public ResponseEntity<?> generateManagerReport(
            @CurrentUser UserDTO currentUser,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            HttpServletRequest request
    ) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Missing Authorization header");
            }

            LocalDate today = LocalDate.now();
            int selectedMonth = (month != null && month >= 1 && month <= 12) ? month : today.getMonthValue();
            int selectedYear = (year != null && year > 2000) ? year : today.getYear();

            LocalDate startDate = LocalDate.of(selectedYear, selectedMonth, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            
            // Manager email: reuse manager id => get from UMS if necessary; here assume currentUser has email
            String managerEmail = "ajay.bhukya@pavestechnologies.com"; // for testing
            if (managerEmail == null || managerEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Manager email not available in token.");
            }
            
            // Generate report data map
            java.util.Map<String, Object> report = managerMonthlyReportService.generateManagerMonthlyReport(
                    currentUser.getId(), startDate, endDate, authHeader
            );
            
            report=ManagerReportDtoAdapter.adapt(report);
            // Generate & send PDF (async can be added later, but now blocking per requirement)
            emailService.generateAndSendManagerPdf(managerEmail, report, currentUser.getName());

            return ResponseEntity.ok("Manager monthly report sent to " + managerEmail);

        } catch (Exception e) {
            // Return meaningful error message
            String msg = "Error generating manager report: " + e.getMessage();
            return ResponseEntity.badRequest().body(msg);
        }
    }
}
