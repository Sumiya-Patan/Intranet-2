package com.intranet.service.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.intranet.entity.TimeSheet;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.service.HolidayExcludeUsersService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimesheetFinanceReportService {
    
    private final TimeSheetRepo timeSheetRepo;
    private final HolidayExcludeUsersService holidayExcludeUsersService;
    public Map<String, Object> getTimesheetFinanceReport() {

        int currentMonth=LocalDate.now().getMonthValue();
        int currentYear=LocalDate.now().getYear();
        List<LocalDate> holidayDates = holidayExcludeUsersService.getUserHolidayDates(currentMonth);

        // ✅ Step 2: Define current month range
        LocalDate firstDay = LocalDate.of(currentYear, currentMonth, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        
        // ✅ Step 4: Fetch all timesheets for this month and year
        List<TimeSheet> allTimeSheets = timeSheetRepo.findByWorkDateBetween(firstDay, lastDay);

        // ✅ Step 5: Filter strictly by same year (safety check)
        List<TimeSheet> currentYearTimeSheets = allTimeSheets.stream()
                .filter(ts -> ts.getWorkDate() != null && ts.getWorkDate().getYear() == currentYear)
                .toList();

         // ✅ Step 6: Calculate total hours (include auto-generated)
        BigDecimal totalHours = currentYearTimeSheets.stream()
                .map(TimeSheet::getHoursWorked)
                .filter(h -> h != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        Map<String, Object> response = Map.of(
            "totalWorkingDays",holidayDates.size(),
            "holidayDates",holidayDates,
            "totalHoursWorked", totalHours
        );

        return response;
    }
    
}
