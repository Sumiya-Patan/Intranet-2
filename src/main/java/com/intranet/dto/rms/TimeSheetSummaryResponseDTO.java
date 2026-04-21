package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheetSummaryResponseDTO {
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
