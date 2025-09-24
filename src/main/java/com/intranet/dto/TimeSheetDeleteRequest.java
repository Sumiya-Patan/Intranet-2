package com.intranet.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheetDeleteRequest {
    private Long timesheetId;
    private List<Long> entryIds;
}
