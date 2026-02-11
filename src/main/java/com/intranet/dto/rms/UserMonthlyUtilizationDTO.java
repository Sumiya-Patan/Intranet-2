package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMonthlyUtilizationDTO {

    private Long userId;

    private Map<String, MonthlySummaryDTO> monthlySummary;
}
