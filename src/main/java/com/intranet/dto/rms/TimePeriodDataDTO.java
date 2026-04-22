package com.intranet.dto.rms;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TimePeriodDataDTO {
    private String period;
    private BigDecimal actual;
    private BigDecimal planned;
    private BigDecimal util;
}
