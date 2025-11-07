package com.intranet.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheetSummaryEmailDTO {
    private Long userId;
    private String email;
    private String userName; // Optional, if available
    private String status;   // APPROVED, REJECTED, SUBMITTED
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalHoursLogged;
    private String approvedBy; // Manager name
    private String reason;     // Reason or comments
}
