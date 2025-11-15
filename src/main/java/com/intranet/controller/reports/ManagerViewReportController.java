package com.intranet.controller.reports;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.controller.external.ManagerSummaryController;
import com.intranet.service.report.ManagerMonthlyReportService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class ManagerViewReportController {

    private final ManagerMonthlyReportService managerMonthlyReportService;

    @GetMapping("/managerMonthly")
    public Map<String, Object> managerMonthlyReport() {

        // Implementation logic to get manager monthly report
        return managerMonthlyReportService.getManagerMonthlyReport(11, 2025);

        
    }
    
    
}
