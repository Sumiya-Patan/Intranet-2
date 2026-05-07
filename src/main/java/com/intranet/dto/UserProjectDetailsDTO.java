package com.intranet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProjectDetailsDTO {
    private Long projectId;
    private String projectName;
    private BigDecimal billableHours;
    private BigDecimal nonBillableHours;
    private BigDecimal totalHours;
    private Double utilizationPercentage;
}
