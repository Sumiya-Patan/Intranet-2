package com.intranet.service.FinanceReportEmail;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class FinancialPdfEmailSender {

    private final JavaMailSender mailSender;

    public void sendFinancialReportPdf(
            String toEmail,
            byte[] pdfBytes,
            String monthName,
            int year,
            String senderName
    ) throws Exception {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(toEmail);
        helper.setSubject("Monthly Financial Report - " + monthName + " " + year);

        helper.setText(
                "Hi " + senderName + ",\n\n" +
                "Your monthly financial report is attached.\n\n" +
                "Regards,\nTimesheet Management System"
        );

        helper.addAttachment("Financial_Report_" + monthName + "_" + year + ".pdf",
                new ByteArrayResource(pdfBytes));

        mailSender.send(message);
    }
}
