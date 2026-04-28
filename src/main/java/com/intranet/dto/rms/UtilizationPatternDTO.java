package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilizationPatternDTO {
    private String id;
    private String patternType; // SUSTAINED_HIGH, SUSTAINED_LOW, VOLATILE, DECLINING, IMPROVING
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    private String scope; // RESOURCE, PROJECT, CLIENT, ROLE, PORTFOLIO
    
    private String title;
    private String description;
    private String impact;
    private String recommendation;
    
    // Target information
    private Long resourceId;
    private String resourceName;
    private Long projectId;
    private String projectName;
    private String clientName;
    private String roleName;
    
    // Pattern details
    private Double averageUtilization;
    private Double utilizationVariance;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationWeeks;
    private List<Double> weeklyUtilizations;
    
    // Threshold breaches
    private Integer weeksOverThreshold;
    private Integer weeksUnderThreshold;
    private Double overThreshold;
    private Double underThreshold;
    
    // Status
    private String status; // ACTIVE, MONITORING, RESOLVED
    private LocalDate detectedDate;
    private LocalDate lastUpdatedDate;
}
