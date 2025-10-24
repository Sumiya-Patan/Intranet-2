package com.intranet.dto.external;

import java.util.List;

import com.intranet.dto.WeekSummaryDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManagerWeeklySummaryDTO {
    private Long userId;
    private String userName;
    private List<WeekSummaryDTO> weeklySummary;
}
