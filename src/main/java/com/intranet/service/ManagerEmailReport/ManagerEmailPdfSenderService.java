package com.intranet.service.ManagerEmailReport;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagerEmailPdfSenderService {

    private final JavaMailSender mailSender;

    public void sendEmailWithAttachment(String to, String subject, String body, byte[] pdf, String fileName) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, false);
        helper.addAttachment(fileName, new ByteArrayResource(pdf));
        mailSender.send(msg);
    }
}
