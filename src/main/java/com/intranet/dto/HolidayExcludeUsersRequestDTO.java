package com.intranet.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class HolidayExcludeUsersRequestDTO  {
   private Long userId;
    private LocalDate holidayDate;
    private String reason;

}
