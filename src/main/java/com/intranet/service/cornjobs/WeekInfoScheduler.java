package com.intranet.service.cornjobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.intranet.service.WeekInfoService;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeekInfoScheduler {

    private final WeekInfoService weekInfoService;

    /**
     * Run at 12:05 AM on 1st day of each month
     * 0 5 0 1 * *
     */
    // @Scheduled(cron = "0 5 0 1 * *")
    // @Scheduled(fixedRate = 10000)
    @EventListener(ApplicationReadyEvent.class)
    public void generateWeeksForLastSixMonths() {

        LocalDate now = LocalDate.now();

        log.info("🕒 Starting WeekInfo generation job for last 6 months...");

        // Loop through last 6 months including current
        for (int i = 0; i < 36; i++) {

            LocalDate targetMonth = now.minusMonths(i);

            int year = targetMonth.getYear();
            int month = targetMonth.getMonthValue();

            log.info("📅 Generating WeekInfo for {}/{}", month, year);

            try {
                weekInfoService.generateWeeksForMonth(year, month);
            } catch (Exception e) {
                log.error("❌ Error generating WeekInfo for {}/{}: {}", month, year, e.getMessage());
            }
        }

        log.info("✅ Completed WeekInfo generation for last 6 months.");
    }



    @Scheduled(cron = "0 0 1 28,27,26 * *")
    public void generateWeeksForNextMonth() {

        LocalDate now = LocalDate.now();

        log.info("🕒 Starting WeekInfo generation next months...");

            LocalDate targetMonth = now.plusMonths(1);

            int year = targetMonth.getYear();
            int month = targetMonth.getMonthValue();

            log.info("📅 Generating WeekInfo for {}/{}", month, year);

            try {
                weekInfoService.generateWeeksForMonth(year, month);
            } catch (Exception e) {
                log.error("❌ Error generating WeekInfo for {}/{}: {}", month, year, e.getMessage());
            }
        

        log.info("✅ Completed WeekInfo generation for next month.");
    }
}
