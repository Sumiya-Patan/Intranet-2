package com.intranet.dto;

import java.util.List;

import lombok.Data;
@Data
public class WeeklySummaryDTO {
    private Long userId;
    private String userName;
    private List<WeekSummaryDTO> weeklySummary;
}
