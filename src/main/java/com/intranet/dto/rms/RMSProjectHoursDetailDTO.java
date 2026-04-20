package com.intranet.dto.rms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class RMSProjectHoursDetailDTO {
    private Long projectId;
    private String projectName;
    private String projectType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal plannedHours;
    private BigDecimal billableHours;
    private BigDecimal nonBillableHours;
    private BigDecimal actualHours;
    private BigDecimal pendingHours;
    private BigDecimal utilizationPercentage;
    private BigDecimal internalHours;
    private List<RMSProjectHoursResourceDTO> resources;
    private List<RMSInternalTaskHoursDTO> internalTasks;
}
