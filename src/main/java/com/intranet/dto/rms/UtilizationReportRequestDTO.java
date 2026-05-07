package com.intranet.dto.rms;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
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
public class UtilizationReportRequestDTO {
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    // Resource filters
    private List<Long> resourceIds;
    private List<String> roles;
    
    // Project filters
    private List<Long> projectIds;
    private List<String> clients;
    
    // Report configuration
    @NotBlank(message = "Report type is required")
    private String reportType; // RESOURCE, PROJECT, CLIENT, ROLE, SUMMARY
    @NotBlank(message = "Group by is required")
    private String groupBy; // DAILY, WEEKLY, MONTHLY
    private boolean includeTrends;
    private boolean includeAlerts;
    
    // Data quality
    @Builder.Default
    private boolean approvedOnly = true; // Default to approved only
    
    // Utilization thresholds
    @Builder.Default
    @Min(value = 1, message = "Over utilization threshold must be at least 1")
    @Max(value = 200, message = "Over utilization threshold cannot exceed 200")
    private Number overUtilizationThreshold = 90.0; // Default 90%
    @Builder.Default
    @Min(value = 1, message = "Under utilization threshold must be at least 1")
    @Max(value = 200, message = "Under utilization threshold cannot exceed 200")
    private Number underUtilizationThreshold = 60.0; // Default 60%
}
