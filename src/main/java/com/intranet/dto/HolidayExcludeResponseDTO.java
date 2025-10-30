package com.intranet.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class HolidayExcludeResponseDTO {
    private Long id;
    private Long userId;
    private String userName;
    private Long managerId;
    private LocalDate holidayDate;
    private String reason;
}
