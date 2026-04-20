package com.intranet.dto.rms;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUtilizationDTO {
    private String projectId;
    private String name;
    private String client;
    private BigDecimal actualHours;
    private BigDecimal plannedHours;
    private Double utilizationPercentage;
    private String healthSignal;
    private BigDecimal billable;
    private BigDecimal nonBillable;
    private BigDecimal internal;
    private String health;
    private String breach;
}
