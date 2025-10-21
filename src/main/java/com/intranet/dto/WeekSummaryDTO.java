package com.intranet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class WeekSummaryDTO {
    private Long weekId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalHours;
    private String weeklyStatus;
    private List<TimeSheetSummaryDTO> timesheets;
}