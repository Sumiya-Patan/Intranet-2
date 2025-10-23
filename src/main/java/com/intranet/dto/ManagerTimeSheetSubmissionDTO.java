package com.intranet.dto;

import java.util.List;

import lombok.Data;

@Data
public class ManagerTimeSheetSubmissionDTO {

    private Long userId;
    private List<TimeSheetEntryCreateDTO> entries;
    
}
