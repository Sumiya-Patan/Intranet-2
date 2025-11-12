package com.intranet.controller.reports;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.service.report.TimesheetFinanceReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class TimeSheetReportFinanceController {

    private final TimesheetFinanceReportService timesheetFinanceReportService;

    @GetMapping("/monthly_finance_real")
    public ResponseEntity<Map<String, Object>> getMonthlyFinanceReport() {

        Map<String, Object> response = timesheetFinanceReportService.getTimesheetFinanceReport(11, 2025);
        return ResponseEntity.ok(response);
    }
    
}
