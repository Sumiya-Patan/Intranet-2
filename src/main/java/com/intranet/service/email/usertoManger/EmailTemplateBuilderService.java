package com.intranet.service.email.usertoManger;

import com.intranet.dto.email.TimeSheetSummaryEmailDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateBuilderService {

    // ✅ Load base URL from application properties / environment
    @Value("${app.frontend.url}")
    private String frontendBaseUrl;

    public String buildTimeSheetSummaryEmail(TimeSheetSummaryEmailDTO dto) {
        String viewLink = frontendBaseUrl != null && !frontendBaseUrl.isEmpty()
                ? frontendBaseUrl
                : ""; // fallback if env not set

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Timesheet Summary Notification</title>
        </head>
        <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color:#f4f6f8; margin:0; padding:0;">
            <table align="center" width="100%%" 
                   style="max-width:600px; background-color:#ffffff; border-radius:10px; box-shadow:0 3px 8px rgba(0,0,0,0.1); overflow:hidden;">
                <tr>
                    <td style="background-color:#0d6efd; padding:20px 30px; text-align:center;">
                        <h1 style="color:#ffffff; margin:0; font-size:22px;">Timesheet Update</h1>
                    </td>
                </tr>
                <tr>
                    <td style="padding:30px;">
                        <h2 style="color:#333333; margin-top:0;">Hello %s,</h2>
                        <p style="font-size:16px; color:#555555;">
                            Here’s a summary of your recent timesheet review:
                        </p>

                        <table style="width:100%%; border-collapse:collapse; margin:20px 0;">
                            <tr style="background-color:#f0f4f8;">
                                <td style="padding:10px 15px; font-weight:bold;">Date Range:</td>
                                <td style="padding:10px 15px;">%s to %s</td>
                            </tr>
                            <tr>
                                <td style="padding:10px 15px; font-weight:bold;">Total Hours Logged:</td>
                                <td style="padding:10px 15px;">%s hours</td>
                            </tr>
                            <tr style="background-color:#f0f4f8;">
                                <td style="padding:10px 15px; font-weight:bold;">Status:</td>
                                <td style="padding:10px 15px; color:%s; font-weight:bold;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding:10px 15px; font-weight:bold;">Reviewed By:</td>
                                <td style="padding:10px 15px;">%s</td>
                            </tr>
                            <tr style="background-color:#f0f4f8;">
                                <td style="padding:10px 15px; font-weight:bold;">Comments:</td>
                                <td style="padding:10px 15px;">%s</td>
                            </tr>
                        </table>

                        <div style="text-align:center; margin:30px 0;">
                            <a href="%s"
                               style="background-color:#0d6efd; color:#ffffff; text-decoration:none;
                                      padding:12px 24px; border-radius:6px; font-size:16px; display:inline-block;">
                                View Timesheet
                            </a>
                        </div>

                        <p style="font-size:15px; color:#666666;">
                            Please log in to the system if any action is required.
                        </p>

                        <p style="margin-top:25px; font-size:14px; color:#888888; border-top:1px solid #e0e0e0; padding-top:10px;">
                            ⚠️ <strong>Note:</strong> If this timesheet requires multiple approvals,
                            please log in to check the complete approval status.
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
                dto.getUserName() != null ? dto.getUserName() : "Employee",
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getTotalHoursLogged(),
                getStatusColor(dto.getStatus()),
                toTitleCase(dto.getStatus()),
                dto.getApprovedBy() != null ? dto.getApprovedBy() : "N/A",
                dto.getReason() != null ? dto.getReason() : "No comments provided.",
                viewLink // 👈 injected dynamically
        );
    }

    private String getStatusColor(String status) {
        if (status == null) return "#333333";
        switch (status.toUpperCase()) {
            case "APPROVED": return "green";
            case "REJECTED": return "red";
            case "SUBMITTED": return "#ff9800";
            default: return "#555555";
        }
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}
