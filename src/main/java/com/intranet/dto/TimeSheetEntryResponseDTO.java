package com.intranet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TimeSheetEntryResponseDTO {

    private Long timesheetEntryId;
    private Long projectId;
    private Long taskId;
    private String description;
    private String workType;
    // private String workLocation;
    private Boolean isBillable;
    private LocalDateTime fromTime;
    private LocalDateTime toTime;
    private BigDecimal hoursWorked;
    private String otherDescription;
}
