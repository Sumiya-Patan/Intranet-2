package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilizationReportResponseDTO {
    
    private LocalDate startDate;
    private LocalDate endDate;
    private String reportType;
    private String groupBy;
    
    // Summary metrics
    private BigDecimal totalHours;
    private BigDecimal plannedHours;
    private BigDecimal utilizationPercentage;
    private BigDecimal billableHours;
    private BigDecimal nonBillableHours;
    private BigDecimal internalHours;
    
    // Resources
    private List<ResourceUtilizationDTO> resourceUtilizations;
    
    // Projects
    private List<ProjectUtilizationDTO> projectUtilizations;
    
    // Clients
    private List<ClientUtilizationDTO> clientUtilizations;
    
    // Roles
    private List<RoleUtilizationDTO> roleUtilizations;
    
    // Trends
    private Map<String, List<PortfolioTrendDTO>> trends;
    
    // Alerts and indicators
    private List<UtilizationAlertDTO> alerts;
    private List<UtilizationPatternDTO> patterns;
    
    // Metadata
    private Integer totalResources;
    private Integer totalProjects;
    private Integer totalClients;
    private Integer totalRoles;
    private BigDecimal confidenceScore;
    private boolean approvedDataOnly;
}
