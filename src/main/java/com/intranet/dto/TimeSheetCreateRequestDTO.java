package com.intranet.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheetCreateRequestDTO {
    private Long userId;
    private LocalDate workDate;
    private List<TimeSheetEntryDTO> entries;
}