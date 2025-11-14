package com.intranet.service.email.missingWeekTimesheet;

import com.intranet.dto.email.MissingTimesheetEmailDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MissingTimesheetEmailTemplateBuilderService {

    @Value("${app.frontend.url}")
    private String frontendBaseUrl;

    public String buildMissingTimesheetEmail(MissingTimesheetEmailDTO dto) {

        String link = (frontendBaseUrl != null && !frontendBaseUrl.isEmpty())
                ? frontendBaseUrl
                : "";

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Timesheet Missing Reminder</title>
        </head>
        <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color:#f5f7fa; margin:0; padding:0;">
            <table align="center" width="100%%" 
                   style="max-width:650px; background-color:#ffffff; border-radius:10px; 
                          box-shadow:0 3px 8px rgba(0,0,0,0.1); overflow:hidden;">
                
                <!-- Header -->
                <tr>
                    <td style="background-color:#dc3545; padding:20px 30px; text-align:center;">
                        <h1 style="color:#ffffff; margin:0; font-size:24px;">Timesheet Submission Reminder</h1>
                    </td>
                </tr>

                <!-- Content -->
                <tr>
                    <td style="padding:30px;">

                        <h2 style="color:#333333; margin-top:0;">Hello %s,</h2>

                        <p style="font-size:16px; color:#555555; line-height:1.6;">
                            Our records indicate that you <strong>did not submit your weekly timesheet</strong> 
                            for the below period:
                        </p>

                        <table style="width:100%%; border-collapse:collapse; margin:20px 0;">
                            <tr style="background-color:#f8d7da;">
                                <td style="padding:10px 15px; font-weight:bold; color:#842029;">Missing Week:</td>
                                <td style="padding:10px 15px; color:#842029;">%s to %s</td>
                            </tr>
                        </table>

                        <p style="font-size:15px; color:#666666; line-height:1.6;">
                            Please log in to the system and complete your timesheet as soon as possible.
                            Timely submission ensures accurate reporting, project tracking, and payroll processing.
                        </p>

                        <div style="text-align:center; margin:30px 0;">
                            <a href="%s"
                               style="background-color:#dc3545; color:#ffffff; text-decoration:none;
                                      padding:12px 28px; border-radius:8px; font-size:16px; display:inline-block;">
                                Submit Timesheet
                            </a>
                        </div>

                        <p style="font-size:14px; color:#888888; margin-top:20px;">
                            If you believe you have already submitted your timesheet, 
                            kindly reach out to the HR team for assistance.
                        </p>

                        <!-- Footer -->
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
                dto.getUserName() != null ? dto.getUserName() : "Employee",
                dto.getStartDate(),
                dto.getEndDate(),
                link
        );
    }
}
