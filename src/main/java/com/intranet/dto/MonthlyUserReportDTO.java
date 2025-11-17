package com.intranet.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyUserReportDTO {

    private Long employeeId;
    private String employeeName;
    private BigDecimal totalHoursWorked;
    private BigDecimal billableHours;
    private BigDecimal nonBillableHours;
    private int activeProjectsCount;
    private LeavesAndHolidaysDTO leavesAndHolidays;
    private List<WeekSummaryDTO> weeklySummaryHistory;
    private Map<String, Double> dayWiseSummary;
    private Map<String, Object>ProjectSummaries;

       

}
