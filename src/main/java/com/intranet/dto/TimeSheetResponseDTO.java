package com.intranet.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class TimeSheetResponseDTO {

    private Long timesheetId;
    private LocalDate workDate;
    private LocalDateTime createdAt;
    private String Status;
    private List<TimeSheetEntryResponseDTO> entries;
}
