package com.intranet.dto.lms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class UserLeaveBreakdown {
    public BigDecimal totalHours;
    public int totalDays;
    public List<LocalDate> leaveDates;

}
