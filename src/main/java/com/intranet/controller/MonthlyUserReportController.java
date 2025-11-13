package com.intranet.controller;

import com.intranet.dto.MonthlyUserReportDTO;
import com.intranet.service.MonthlyUserReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class MonthlyUserReportController {

    private final MonthlyUserReportService reportService;

    @GetMapping("/user/{employeeId}/monthly")
    public MonthlyUserReportDTO getMonthlyReport(
            @PathVariable Long employeeId,
            @RequestParam int month,
            @RequestParam int year) {

        return reportService.getMonthlyUserReport(employeeId, month, year);
    }
}
