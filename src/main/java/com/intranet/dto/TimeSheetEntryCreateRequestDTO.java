package com.intranet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheetEntryCreateRequestDTO {
    private Long projectId;
    private Long taskId;
    private String description;
    private String workType;
    private BigDecimal hoursWorked;
    private LocalDateTime fromTime;
    private LocalDateTime toTime;
    private String otherDescription;
}
