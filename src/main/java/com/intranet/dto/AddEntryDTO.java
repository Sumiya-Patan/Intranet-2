package com.intranet.dto;

import java.util.List;

import lombok.Data;

@Data
public class AddEntryDTO {
    private Long timeSheetId;
    private List<TimeSheetEntryCreateDTO> entries;
    
}
