package com.intranet.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TimeSheetCreateDTO {
    private Long userId;
    private LocalDate workDate;
    private BigDecimal hoursWorked; // optional, backend will calculate if null
    private List<TimeSheetEntryCreateDTO> entries;
}
