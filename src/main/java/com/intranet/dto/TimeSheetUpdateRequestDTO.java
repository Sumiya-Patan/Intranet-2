package com.intranet.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheetUpdateRequestDTO {
    private LocalDate workDate;
    // private String status;

    private List<TimeSheetEntryUpdateDTO> entries;
}
