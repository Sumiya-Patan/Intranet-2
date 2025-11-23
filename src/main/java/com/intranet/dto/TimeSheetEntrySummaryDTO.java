package com.intranet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TimeSheetEntrySummaryDTO {
    private Long timesheetEntryid;
    private Long projectId;
    private String projectName;
    private Long taskId;
    private String taskName;
    private String description;
    private String workLocation;
    private LocalDateTime fromTime; // optional: formatted as HH:mm
    private LocalDateTime toTime;   // optional: formatted as HH:mm
    private BigDecimal hoursWorked;
    private String otherDescription;
    private Boolean isBillable;
}