package com.intranet.service.email.MonthEndRemainder;

import com.intranet.dto.email.MissingTimesheetEmailDTO;
import com.intranet.service.email.ums_corn_job_token.UmsAuthService;
import com.intranet.util.EmailUtil;
import com.intranet.util.cache.UserDirectoryService;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonthlyTimesheetReminderService {

    @Value("${timesheet.user}")
    private String umsEmail;

    private final UserDirectoryService userDirectoryService;
    private final UmsAuthService umsAuthService;
    private final EmailUtil emailUtil;
    private final MonthEndTimesheetEmailTemplateBuilderService monthEndEmailTemplateBuilderService;

    /**
     * Runs every month on the 20th at 9 AM
        */
    @Scheduled(cron = "0 0 16 20 * *", zone = "Asia/Kolkata")
    public void sendMonthlyTimesheetReminder() {

        System.out.println("📅 Monthly Timesheet Reminder Cron Started");

        // 🔐 Step 1: Get token from UMS
        String token = umsAuthService.getUmsToken();
        if (token == null) {
            System.out.println("❌ Unable to obtain UMS token.");
            return;
        }

        String authHeader = "Bearer " + token;

        // ✔ Step 2: Fetch all UMS users
        Map<Long, Map<String, Object>> allUsers =
                userDirectoryService.fetchAllUsers(authHeader);

        if (allUsers == null || allUsers.isEmpty()) {
            System.out.println("⚠ No users found from UMS.");
            return;
        }

        // Current month
        YearMonth current = YearMonth.now();
        LocalDate start = current.atDay(1);
        LocalDate end   = current.atEndOfMonth();

        // ✔ Step 3: Email ALL users
        for (Long userId : allUsers.keySet()) {

            Map<String, Object> user = allUsers.get(userId);
            if (user == null) continue;

            String email = (String) user.get("email");
            String name  = (String) user.get("name");

            if (email == null || email.isBlank()) continue;

            // Prepare email DTO
            MissingTimesheetEmailDTO dto = new MissingTimesheetEmailDTO();
            dto.setUserName(name);
            dto.setStartDate(start.toString());
            dto.setEndDate(end.toString());

            // Generate HTML email template
            String html = monthEndEmailTemplateBuilderService.buildMonthEndReminderEmail(dto);


            try {
                // Send email
                emailUtil.sendEmail(
                        email,
                        "⏰ Monthly Reminder — Submit Your Timesheet",
                        html
                );

                System.out.println("📧 Reminder sent: " + email);
            } catch (Exception ex) {
                System.out.println("❌ Failed sending to " + email + " : " + ex.getMessage());
                try {
                    emailUtil.sendEmail(umsEmail, "Monthly Timesheet Reminder Failed", "<h1>Failed to send monthly timesheet reminder to " + email + "</h1><p>Error: " + ex.getMessage() + "</p>");
                } catch (MessagingException e) {
                   System.out.println("❌ Failed sending to " + email + " : " + ex.getMessage());
                }
            }
        }

        System.out.println("✅ Monthly Timesheet Reminder Completed.");
    }
}
