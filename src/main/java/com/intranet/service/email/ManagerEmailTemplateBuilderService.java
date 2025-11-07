package com.intranet.service.email;

import com.intranet.dto.email.WeeklySubmissionEmailDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ManagerEmailTemplateBuilderService {

    @Value("${app.frontend.url}")
    private String frontendBaseUrl;

    public String buildWeeklySubmissionEmail(WeeklySubmissionEmailDTO dto) {
        String viewLink = frontendBaseUrl != null && !frontendBaseUrl.isEmpty()
                ? frontendBaseUrl
                : "";

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Weekly Timesheet Submission</title>
        </head>
        <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color:#f5f7fa; margin:0; padding:0;">
            <table align="center" width="100%%" 
                   style="max-width:650px; background-color:#ffffff; border-radius:10px; 
                          box-shadow:0 3px 8px rgba(0,0,0,0.1); overflow:hidden;">
                <tr>
                    <td style="background-color:#0d6efd; padding:20px 30px; text-align:center;">
                        <h1 style="color:#ffffff; margin:0; font-size:24px;">Weekly Timesheet Submission</h1>
                    </td>
                </tr>

                <tr>
                    <td style="padding:30px;">
                        <h2 style="color:#333333; margin-top:0;">Hello %s,</h2>

                        <p style="font-size:16px; color:#555555; line-height:1.6;">
                            Your team member <strong>%s</strong> has submitted their 
                            <strong>weekly timesheet</strong> for your review.
                        </p>

                        <table style="width:100%%; border-collapse:collapse; margin:20px 0;">
                            <tr style="background-color:#f0f4f8;">
                                <td style="padding:10px 15px; font-weight:bold;">Employee Name:</td>
                                <td style="padding:10px 15px;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding:10px 15px; font-weight:bold;">Date Range:</td>
                                <td style="padding:10px 15px;">%s to %s</td>
                            </tr>
                            <tr style="background-color:#f0f4f8;">
                                <td style="padding:10px 15px; font-weight:bold;">Total Hours Logged:</td>
                                <td style="padding:10px 15px;">%s hours</td>
                            </tr>
                        </table>

                        <div style="text-align:center; margin:30px 0;">
                            <a href="%s"
                               style="background-color:#0d6efd; color:#ffffff; text-decoration:none;
                                      padding:12px 28px; border-radius:8px; font-size:16px; display:inline-block;">
                                Review Timesheet
                            </a>
                        </div>

                        <p style="font-size:15px; color:#666666; line-height:1.6;">
                            Please log in to the system to review and approve this submission.
                            Timely approval ensures accurate reporting and payroll processing.
                        </p>

                        <p style="margin-top:30px; font-size:14px; color:#888888;">
                            Regards,<br>
                            <strong>Timesheet Management Team</strong><br>
                            <span style="font-size:12px; color:#aaaaaa;">Automated Notification</span>
                        </p>
                    </td>
                </tr>

                <tr>
                    <td style="background-color:#f0f0f0; text-align:center; padding:15px; font-size:12px; color:#999999;">
                        © 2025 Timesheet Management System. All rights reserved.
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(
                dto.getManagerName() != null ? dto.getManagerName() : "Manager",
                dto.getUserName() != null ? dto.getUserName() : "Employee",
                dto.getUserName() != null ? dto.getUserName() : "Employee",
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getTotalHoursLogged() != null ? dto.getTotalHoursLogged() : 0,
                viewLink
        );
    }
}
