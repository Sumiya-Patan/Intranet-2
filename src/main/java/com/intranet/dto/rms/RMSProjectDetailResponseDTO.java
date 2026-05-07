package com.intranet.dto.rms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class RMSProjectDetailResponseDTO {
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
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private Integer assignedResourceCount;
    private Integer loggedResourceCount;
    private List<RMSProjectMemberDTO> assignedResources;
    private List<RMSProjectHoursResourceDTO> resources;
    private List<RMSProjectTaskHoursDTO> tasks;
    private List<RMSInternalTaskHoursDTO> internalTasks;
}
