package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleUtilizationDTO {
    private String roleName;
    
    // Hours breakdown
    private BigDecimal totalHours;
    private BigDecimal billableHours;
    private BigDecimal nonBillableHours;
    private BigDecimal internalHours;
    private BigDecimal plannedHours;
    
    // Resource metrics
    private Integer uniqueResources;
    private BigDecimal averageHoursPerResource;
    
    // Utilization metrics
    private BigDecimal utilizationPercentage;
    private BigDecimal billableRatio;
    
    // Visual indicators
    private String utilizationBand; // OPTIMAL, HIGH, LOW, CRITICAL
    private String trendSignal; // UP, DOWN, STABLE
    private List<String> alerts;
    
    // Pattern analysis
    private boolean consistentlyOverUtilized;
    private boolean consistentlyUnderUtilized;
    private Integer consecutiveWeeksOverThreshold;
    private Integer consecutiveWeeksUnderThreshold;
    
    // Data quality
    private Integer confidenceScore;
    private Integer daysWithApprovedTimesheets;
    private Integer totalWorkingDays;
}
