package com.intranet.service.cornjobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.intranet.entity.CronJobExecutionLog;
import com.intranet.repository.CronJobExecutionLogRepo;
import com.intranet.service.WeekInfoService;
import com.intranet.util.EmailUtil;

import jakarta.mail.MessagingException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeekInfoScheduler {

    private final WeekInfoService weekInfoService;
    private final EmailUtil emailUtil;
    private final CronJobExecutionLogRepo cronJobExecutionLogRepo;

    @Value("${timesheet.user}")
    private String adminEmail;

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

        CronJobExecutionLog logEntry = CronJobExecutionLog.builder()
            .jobName("GenerateWeeksForNextMonth")
            .startTime(LocalDateTime.now())
            .status(CronJobExecutionLog.Status.RUNNING)
            .message("Execution started")
            .build();

            cronJobExecutionLogRepo.save(logEntry);

            LocalDate targetMonth = now.plusMonths(1);

            int year = targetMonth.getYear();
            int month = targetMonth.getMonthValue();

            log.info("📅 Generating WeekInfo for {}/{}", month, year);

            try {
                weekInfoService.generateWeeksForMonth(year, month);
                logEntry.setEndTime(LocalDateTime.now());
                logEntry.setStatus(CronJobExecutionLog.Status.SUCCESS);
                logEntry.setMessage("Generated successfully");
                cronJobExecutionLogRepo.save(logEntry);
            } 
            catch (Exception e) {
                log.error("❌ Error generating WeekInfo for {}/{}: {}", month, year, e.getMessage());

                logEntry.setEndTime(LocalDateTime.now());
                logEntry.setStatus(CronJobExecutionLog.Status.FAILED);
                logEntry.setMessage(e.getMessage());
                cronJobExecutionLogRepo.save(logEntry);


                    String subject = String.format(
                    "Error generating WeekInfo for %d/%d: %s",
                    month, year, e.getMessage()
            );

            String body = "<h3>WeekInfo Generation Error</h3>"
                    + "<p>An error occurred while generating WeekInfo.</p>"
                    + "<p><b>Error:</b> " + e.getMessage() + "</p>";

            try {
                emailUtil.sendEmail(adminEmail, subject, body);
            } catch (MessagingException e1) {
                log.error("❌ Failed to send error email to admin: {}", e1.getMessage());
            }

            }
        

        log.info("✅ Completed WeekInfo generation for next month.");
    }
}
