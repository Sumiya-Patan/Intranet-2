package com.intranet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class LeavesAndHolidaysDTO {

    private BigDecimal totalLeavesHours;
    private int totalHolidays;
    private List<LocalDate> leaveDates;
    private List<LocalDate> holidayDates;
}
