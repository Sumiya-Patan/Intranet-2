package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryResponseDTO {
    // Month/Period information
    private String periodLabel;
    private String startDate;
    private String endDate;
    
    // Overall KPIs for the period
    private BigDecimal overallUtilizationPercentage;
    private BigDecimal actualHours;
    private BigDecimal plannedHours;
    
    // All resource summaries for the period
    private List<ResourceSummaryDTO> resourceSummaries;
    
    // KPI Statistics
    private List<KPIStatDTO> kpiStats;
    
    // Portfolio trends
    private Map<String, List<PortfolioTrendDTO>> portfolioTrends;
    
    // Hour breakdown percentages
    private double billablePercentage;
    private double internalNonBillablePercentage;
    private double otherNonBillablePercentage;
    private double totalPercentage;
    
    // Additional metrics
    private Integer totalResources;
    private BigDecimal totalBillableHours;
    private BigDecimal totalNonBillableHours;
    private BigDecimal totalInternalHours;
    private Integer averageConfidenceScore;
}
