package com.intranet.dto;

import lombok.Data;
import java.util.List;

@Data
public class TimeSheetBulkReviewRequestDTO {
    private Long userId;                 // Whose timesheets are being reviewed
    private List<Long> timesheetIds;     // List of timesheet IDs to approve/reject
    private String status;               // APPROVED or REJECTED
    private String comments;             // Optional
}
