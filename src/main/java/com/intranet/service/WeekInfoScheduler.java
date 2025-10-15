package com.intranet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeekInfoScheduler {

    private final WeekInfoService weekInfoService;

    /**
     * Cron: run at 12:05 AM on the 1st day of each month
     * 0 5 0 1 * *  → (second=0, minute=5, hour=0, day=1, month=*, dayOfWeek=*)
     */
    // @Scheduled(cron = "0 5 0 1 * *")
    @Scheduled(fixedRate = 10000)
    public void generateCurrentMonthWeeks() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        log.info("🕒 Starting monthly WeekInfo generation job for {}/{}", month, year);
        weekInfoService.generateWeeksForMonth(year, month);
        log.info("✅ Finished monthly WeekInfo generation job for {}/{}", month, year);
    }
}
