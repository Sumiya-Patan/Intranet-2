package com.intranet.service.cornjobs;

import com.intranet.service.FinanceReportEmail.FinancialPdfEmailSender;
import com.intranet.service.FinanceReportEmail.FinancialPdfGeneratorService;
import com.intranet.service.FinanceReportEmail.FinancialPdfTemplateBuilder;
import com.intranet.service.email.ums_corn_job_token.UmsAuthService;
import com.intranet.service.report.TimesheetFinanceReportService;
import com.intranet.entity.CronJobExecutionLog;
import com.intranet.entity.EmailSettings;
import com.intranet.repository.CronJobExecutionLogRepo;
import com.intranet.repository.EmailSettingsRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceReportMonthlyCronService {

    private final TimesheetFinanceReportService financeService;
    private final FinancialPdfTemplateBuilder templateBuilder;
    private final FinancialPdfGeneratorService pdfGenerator;
    private final FinancialPdfEmailSender emailSender;
    private final CronJobExecutionLogRepo cronLogRepo;
    private final EmailSettingsRepo emailSettingsRepository;
    private final UmsAuthService umsAuthService;

    /**
     *  Runs at 6:00 AM on the last day of every month
     *  Cron Expression: 0 0 6 L * ?
     */
    @Transactional
    @Scheduled(cron = "0 0 1 1 * ?")
    public void sendMonthlyFinanceReport() {

        List<EmailSettings> emailSettingsList = emailSettingsRepository.findAll();

        LocalDate today = LocalDate.now();
        int  month = today.minusMonths(1).getMonthValue();
        int year = today.getYear();

        log.info("📊 Finance Report Cron Started for month: {}/{}", month, year);

        // 📝 Create log entry
        CronJobExecutionLog logEntry = cronLogRepo.save(
                CronJobExecutionLog.builder()
                        .jobName("MonthlyFinanceReportPDF")
                        .startTime(LocalDateTime.now())
                        .status(CronJobExecutionLog.Status.RUNNING)
                        .message("Started monthly finance report process")
                        .build()
        );

        try {

            // 🔐 Auto-login to UMS
            String token = umsAuthService.getUmsToken();
            if (token==null) {
                logEntry.setEndTime(LocalDateTime.now());
                logEntry.setStatus(CronJobExecutionLog.Status.FAILED);
                logEntry.setMessage("Failed to auto-login to UMS");
                cronLogRepo.save(logEntry);
                return;
            }
            String authHeader = "Bearer " + token;


            String financeAdminEmail = emailSettingsList.stream()
                .map(EmailSettings::getEmail)
                .filter(email -> email != null && !email.trim().isEmpty())
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No valid finance admin email found in EmailSettings table"));

            Map<String, Object> financeData = financeService.getTimesheetFinanceReportAutoEmail(month, year, authHeader);

            String monthName = java.time.Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            String html = templateBuilder.buildFinanceHtml(financeData, monthName, year);
            byte[] pdfBytes = pdfGenerator.generatePdfFromHtml(html);

            emailSender.sendFinancialReportPdf(financeAdminEmail, pdfBytes, monthName, year, "Finance Team");

            // Update cron log success
            logEntry.setEndTime(LocalDateTime.now());
            logEntry.setStatus(CronJobExecutionLog.Status.SUCCESS);
            logEntry.setMessage("Finance report successfully emailed to " + financeAdminEmail);
            cronLogRepo.save(logEntry);

            log.info("✔ Finance Report Email Sent Successfully to {}", financeAdminEmail);

        }  catch (Exception e) {

        // ❌ FAILURE - Update log entry
        logEntry.setEndTime(LocalDateTime.now());
        logEntry.setStatus(CronJobExecutionLog.Status.FAILED);
        logEntry.setMessage("Error: " + e.getMessage());
        cronLogRepo.save(logEntry);

        log.error("❌ Finance Report Cron Failed: {}", e.getMessage(), e);
    }
    }
}
