package com.intranet.controller.reports;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/monthly_finance")
    public ResponseEntity<Map<String, Object>> getMonthlyFinanceReport(
        @PathVariable(required = false) Integer month,
        @PathVariable(required = false) Integer year
        ) {

        // If month/year are missing → set current month/year
        if (month == null || year == null) {
            month = java.time.LocalDate.now().getMonthValue();
            year = java.time.LocalDate.now().getYear();
        }

        if (month < 1 || month > 12 || year < 2000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid month or year parameter"));
        }

        else if (month<= 0 || year<=0 || month > 12 || year < 2000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Month and Year parameters are required"));
        }
        try{

            Map<String, Object> response = timesheetFinanceReportService.getTimesheetFinanceReport(month, year);
                    return ResponseEntity.ok(response);
        }
        catch(Exception e){
                return ResponseEntity.badRequest().body(Map.of("error", "Internal server error"));
        }
        
    }
    
}
