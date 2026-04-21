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
public class ResourceSummaryDTO {
    private Long userId;
    private String name;
    private BigDecimal billableHours;
    private BigDecimal nonBillableHours;
    private BigDecimal internalHours;
    private BigDecimal totalHours;
    private BigDecimal plannedCapacity;
    private Double utilizationPercentage;
    private Integer confidenceScore;
    private String resourceContext;
    private String hourlySplit;
    private String trendSignal;
    private Double finalUtilPercentage;
}
