package com.intranet.service.email.managerReviews;

import com.intranet.dto.email.TimeSheetSummaryEmailDTO;
import com.intranet.service.email.usertoManger.EmailTemplateBuilderService;
import com.intranet.util.EmailUtil;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeSheetNotificationService {

    private final EmailUtil emailUtil;
    private final EmailTemplateBuilderService emailTemplateBuilderService;

    /**
     * Sends professional timesheet summary emails to multiple users.
     */
    public void sendTimeSheetSummaryEmails(List<TimeSheetSummaryEmailDTO> summaries) {
        for (TimeSheetSummaryEmailDTO dto : summaries) {
            try {
                String htmlContent = emailTemplateBuilderService.buildTimeSheetSummaryEmail(dto);
                String subject = "Timesheet " + dto.getStatus() + " – " +
                                 dto.getStartDate() + " to " + dto.getEndDate();

                emailUtil.sendEmail(dto.getEmail(), subject, htmlContent);
                System.out.println("✅ Email sent to: " + dto.getEmail());
            } catch (MessagingException e) {
                System.err.println("❌ Failed to send email to " + dto.getEmail() + ": " + e.getMessage());
            }
        }
    }
}
