package com.intranet.dto.rms;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class RMSProjectHoursResourceDTO {
    private Long resourceId;
    private String resourceName;
    private String resourceEmail;
    private BigDecimal billableHours;
    private BigDecimal nonBillableHours;
    private BigDecimal actualHours;
}
