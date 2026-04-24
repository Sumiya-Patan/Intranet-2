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
public class TimeSheetSummaryResponseDTO {
        private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalHours;
    private BigDecimal billableHours;
    private BigDecimal nonBillableHours;
    private List<RMSProjectHoursDTO> projectHours;
private Long totalUsers;
    private BigDecimal averageTotalHours;
    private BigDecimal averageBillableHours;
    private BigDecimal averageNonBillableHours;
    private List<TimePeriodDataDTO> daily;
    private List<TimePeriodDataDTO> weekly;
    private List<TimePeriodDataDTO> monthly;
    private Long totalResources;
    private BigDecimal utilization;
    private BigDecimal billableRatio;
    private BigDecimal confidenceScore;
    private List<ResourceSummaryDTO> resourceSummaries;
    private List<KPIStatDTO> kpiStats;
    private Map<String, List<PortfolioTrendDTO>> portfolioTrends;
    private List<ProjectUtilizationDTO> projects;
    private List<AlertDTO> alerts;
    private double billablePercentage;
    private double internalNonBillablePercentage;
    private double otherNonBillablePercentage;
    private double totalPercentage;
}
