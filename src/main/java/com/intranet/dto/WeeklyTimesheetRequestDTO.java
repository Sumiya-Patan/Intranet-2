package com.intranet.dto;

import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTimesheetRequestDTO {
    private LocalDate startDate;
    private LocalDate endDate;
}
