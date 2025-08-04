package com.intranet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// TimeSheetEntryDTO.java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeSheetEntryDTO {
    private Long projectId;
    private Long taskId;
    private String description;
    private String workType;
    private LocalDateTime fromTime;
    private LocalDateTime toTime;
    private BigDecimal hoursWorked;
    private String otherDescription;
}
