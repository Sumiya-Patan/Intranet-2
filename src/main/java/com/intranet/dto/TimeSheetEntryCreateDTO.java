package com.intranet.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TimeSheetEntryCreateDTO {
    private Long projectId;
    private Long taskId;
    private String description;
    private String workLocation;
    private LocalDateTime fromTime;
    private LocalDateTime toTime;
    private BigDecimal hoursWorked; 
    private String otherDescription;
    private boolean isBillable;
}
