package com.intranet.service.email.managerReviews;

import com.intranet.dto.email.WeeklySubmissionEmailDTO;
import com.intranet.util.EmailUtil;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerNotificationEmailService {

    private final EmailUtil emailUtil;
    private final ManagerEmailTemplateBuilderService templateBuilderService;

    /**
     * Sends weekly submission emails to managers.
     */
    public void sendWeeklySubmissionEmails(List<WeeklySubmissionEmailDTO> submissions) {
        for (WeeklySubmissionEmailDTO dto : submissions) {
            try {
                String htmlContent = templateBuilderService.buildWeeklySubmissionEmail(dto);
                String subject = String.format("Timesheet Submitted by %s (%s to %s)",
                        dto.getUserName(),
                        dto.getStartDate(),
                        dto.getEndDate());

                emailUtil.sendEmail(dto.getManagerEmail(), subject, htmlContent);
                System.out.printf("✅ Weekly submission email sent to manager: %s%n", dto.getManagerEmail());
            } catch (MessagingException e) {
                System.err.printf("❌ Failed to send weekly submission email to %s: %s%n",
                        dto.getManagerEmail(), e.getMessage());
            }
        }
    }
}
