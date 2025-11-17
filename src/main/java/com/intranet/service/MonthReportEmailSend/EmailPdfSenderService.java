package com.intranet.service.MonthReportEmailSend;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailPdfSenderService {

    private final JavaMailSender mailSender;

    public void sendPdfReport(String toEmail, byte[] pdfBytes, String employeeName) throws Exception {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(toEmail);
        helper.setSubject("Monthly Timesheet PDF Report - " + employeeName);
        helper.setText("Hi,\n\nPlease find attached your monthly timesheet report.\n\nRegards,\nTimesheet System");

        helper.addAttachment("Monthly_Report.pdf", new ByteArrayResource(pdfBytes));

        mailSender.send(message);
    }
}
