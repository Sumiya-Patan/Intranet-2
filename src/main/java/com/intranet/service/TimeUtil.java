package com.intranet.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class TimeUtil {

    // Converts fromTime and toTime into HH.MM
    public static BigDecimal calculateHours(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) return BigDecimal.ZERO;
        if (to.isBefore(from)) throw new IllegalArgumentException("toTime cannot be before fromTime");

        Duration duration = Duration.between(from, to);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;

        // Format as HH.MM
        String hhmm = String.format("%02d.%02d", hours, minutes);
        return new BigDecimal(hhmm);
    }

    // Sum list of HH.MM formatted hours
    public static BigDecimal sumHours(List<BigDecimal> hoursList) {
        int totalHours = 0;
        int totalMinutes = 0;

        for (BigDecimal h : hoursList) {
            String[] parts = h.toString().split("\\.");
            totalHours += Integer.parseInt(parts[0]);
            totalMinutes += Integer.parseInt(parts[1]);
        }

        // Convert minutes > 60 to hours
        totalHours += totalMinutes / 60;
        totalMinutes = totalMinutes % 60;

        String hhmm = String.format("%02d.%02d", totalHours, totalMinutes);
        return new BigDecimal(hhmm);
    }
}
