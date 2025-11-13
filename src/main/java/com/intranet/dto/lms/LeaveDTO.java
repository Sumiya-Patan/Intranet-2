package com.intranet.dto.lms;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveDTO {

    private Long employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String status;
}
