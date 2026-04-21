package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSummaryMinimalDTO {
    private String resourceContext;
    private String hourlySplit;
    private String trendSignal;
    private Double finalUtilPercentage;
}
