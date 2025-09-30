package com.intranet.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class StartEndDateReqDTO {
    private LocalDate startDate;
    private LocalDate endDate;
}
