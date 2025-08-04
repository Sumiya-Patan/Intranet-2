package com.intranet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TimeSheetEntryUpdateDTO {
    private Long timesheetEntryId; // required to identify entry
    private Long projectId;
    private Long taskId;
    private String description;
    private String workType;
    private BigDecimal hoursWorked;
    private LocalDateTime fromTime;
    private LocalDateTime toTime;
    private String otherDescription;
}
