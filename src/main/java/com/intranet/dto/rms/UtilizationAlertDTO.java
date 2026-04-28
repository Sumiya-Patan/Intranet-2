package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilizationAlertDTO {
    private String id;
    private String type; // OVER_UTILIZATION, UNDER_UTILIZATION, PATTERN, DATA_QUALITY
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    private String scope; // RESOURCE, PROJECT, CLIENT, ROLE, PORTFOLIO
    
    private String title;
    private String message;
    private String recommendation;
    
    // Target information
    private Long resourceId;
    private String resourceName;
    private Long projectId;
    private String projectName;
    private String clientName;
    private String roleName;
    
    // Alert details
    private Double currentValue;
    private Double thresholdValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer consecutiveDays;
    private Integer consecutiveWeeks;
    
    // Status
    private String status; // OPEN, ACKNOWLEDGED, RESOLVED
    private LocalDate createdDate;
    private LocalDate acknowledgedDate;
    private LocalDate resolvedDate;
}
