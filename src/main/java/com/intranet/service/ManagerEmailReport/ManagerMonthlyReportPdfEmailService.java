package com.intranet.service.ManagerEmailReport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManagerMonthlyReportPdfEmailService {

    private final ManagerPdfTemplateBuilder templateBuilder;
    private final com.intranet.service.MonthReportEmailSend.PdfGeneratorService pdfGenerator; // reuse existing PdfGeneratorService
    private final ManagerEmailPdfSenderService emailSender; // created below

    /**
     * Generate HTML from report, convert to PDF and send as attachment.
     */
    public void generateAndSendManagerPdf(String toEmail, Map<String, Object> report, String managerName) throws Exception {

        // 1) Build XHTML-safe HTML string
        String html = templateBuilder.buildManagerMonthlyReportHtml(report, managerName);
        
        // 2) Create PDF bytes
        byte[] pdf = pdfGenerator.generatePdfFromHtml2(html);

        // 3) Email subject/body
        String subject = "Manager Monthly Report - " + managerName;
        String body = "Hi " + managerName + ",\n\nPlease find attached the manager monthly report.\n\nRegards,\nTimesheet Management System";

        // 4) Send email with attachment (file name includes month-year if available)
        String filename = "Manager_Report.pdf";
        emailSender.sendEmailWithAttachment(toEmail, subject, body, pdf, filename);
    }
}
