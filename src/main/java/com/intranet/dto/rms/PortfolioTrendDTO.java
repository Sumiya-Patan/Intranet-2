package com.intranet.dto.rms;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioTrendDTO {
    private String period;
    private Integer actual;
    private Integer planned;
    private Double util;
}
