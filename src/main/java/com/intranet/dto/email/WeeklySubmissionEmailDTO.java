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
public class WeeklySubmissionEmailDTO {
    private Long managerId;
    private String managerName;
    private String managerEmail;

    private Long userId;
    private String userName;

    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal totalHoursLogged;
}
