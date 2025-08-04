package com.intranet.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class TimeSheetResponseDTO {

    private Long timesheetId;
    private Long userId;
    private LocalDate workDate;
    private String Status;
    private List<TimeSheetEntryResponseDTO> entries;
}
