package com.intranet.dto.rms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
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
}