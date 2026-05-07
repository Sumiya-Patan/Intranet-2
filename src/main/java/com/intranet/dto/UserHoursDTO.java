package com.intranet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserHoursDTO {
    private Long userId;
    private String userName;
    private BigDecimal billableHours;
    private BigDecimal nonBillableHours;
    private String designation;
    private Double billablePercentage;
}
