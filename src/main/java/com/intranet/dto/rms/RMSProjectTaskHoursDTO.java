package com.intranet.dto.rms;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class RMSProjectTaskHoursDTO {
    private Long taskId;
    private String taskName;
    private Boolean billable;
    private BigDecimal billableHours;
    private BigDecimal nonBillableHours;
    private BigDecimal actualHours;
    private List<RMSProjectHoursResourceDTO> resources;
}
