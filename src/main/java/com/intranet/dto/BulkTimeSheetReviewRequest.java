package com.intranet.dto;

import java.util.List;
import lombok.Data;

@Data
public class BulkTimeSheetReviewRequest {
    private List<Long> timesheetIds;
    private String status;
    private String comment; // optional, required if REJECTED
}
