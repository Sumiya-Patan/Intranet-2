package com.intranet.controller.reports;

import com.intranet.dto.MonthlyUserReportDTO;
import com.intranet.service.MonthlyUserReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class MonthlyUserReportController {

    private final MonthlyUserReportService reportService;

    @GetMapping("/user/monthly")
    public MonthlyUserReportDTO getMonthlyReport(
            @RequestParam int month,
            @RequestParam int year) {
        

        return reportService.getMonthlyUserReport(17L, month, year);
    }
}
