package com.intranet.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TimeSheetEntrySummaryDTO {
    private Long projectId;
    private Long taskId;
    private String description;
    private String workLocation;
    private String fromTime; // optional: formatted as HH:mm
    private String toTime;   // optional: formatted as HH:mm
    private BigDecimal hoursWorked;
    private String otherDescription;
}