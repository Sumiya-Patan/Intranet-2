package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResourceDataDTO {
    private Long userId;
    private String userName;
    private String resourceContext;
    private String hourlySplit;
    private String trendSignal;
    private Double finalUtilPercentage;
}
