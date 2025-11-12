package com.intranet.controller.reports;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.intranet.service.HolidayExcludeUsersService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class TimeSheetReportFinance {

    private final HolidayExcludeUsersService holidayExcludeUsersService;
    @GetMapping("/monthly_finance_real")
    public ResponseEntity<Map<String, Object>> getMonthlyFinanceReport() {

        int monthValue=LocalDate.now().getMonthValue();
        List<LocalDate> holidayDates = holidayExcludeUsersService.getUserHolidayDates(monthValue);
        Map<String, Object> response = Map.of(
            "totalWorkingDays",holidayDates.size(),
            "holidayDates",holidayDates
        );
        return ResponseEntity.ok(response);
    }
    
}
