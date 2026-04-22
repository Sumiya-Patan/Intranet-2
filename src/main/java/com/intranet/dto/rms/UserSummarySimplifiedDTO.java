package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummarySimplifiedDTO {
    private Long userId;
    private String name;
    private String resourceContext;
    private String hourlySplit;
    private String trendSignal;
    private Double finalUtilPercentage;
}
