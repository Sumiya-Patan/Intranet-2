package com.intranet.dto.rms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class RMSProjectHoursSummaryResponseDTO {
    private LocalDate generatedOn;
    private Integer totalProjects;
    private BigDecimal totalPlannedHours;
    private BigDecimal totalBillableHours;
    private BigDecimal totalNonBillableHours;
    private BigDecimal totalActualHours;
    private BigDecimal totalPendingHours;
    private List<RMSProjectHoursDetailDTO> projects;
}
