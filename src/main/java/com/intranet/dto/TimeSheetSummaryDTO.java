package com.intranet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class TimeSheetSummaryDTO {
    private Long timesheetId;
    private LocalDate workDate;
    private BigDecimal hoursWorked;
    private String status;
    private List<TimeSheetEntrySummaryDTO> entries;
    private List<ActionStatusDTO> actionStatus;
}