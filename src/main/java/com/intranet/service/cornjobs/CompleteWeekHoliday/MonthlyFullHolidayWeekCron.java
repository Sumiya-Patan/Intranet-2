package com.intranet.service.cornjobs.CompleteWeekHoliday;


import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonthlyFullHolidayWeekCron {

    private final FullHolidayWeekProcessorService processorService;

    /**
     * Runs monthly on 1st at 1 AM
     * Cron: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 0 1,2 1,2,3 * ?", zone = "Asia/Kolkata")
    // @Scheduled(cron = "0 */2 * * * *")
    public void runMonthlyFullHolidayProcessing() {
        System.out.println("🟢 Monthly Full Holiday Week Cron Started...");
        processorService.processMonth();
        System.out.println("🟢 Monthly Full Holiday Week Cron Completed.");
    }
}
