package com.intranet.controller.email;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.intranet.service.email.EmailService;

@RestController
@RequestMapping("/api/mail")
@CrossOrigin(origins = "*",allowedHeaders = "*")
@RequiredArgsConstructor
public class MailController {

    private final EmailService emailService;

    @PostMapping("/send-reminder")
    public ResponseEntity<String> sendReminders(@RequestBody List<String> emailList) {
        if (emailList == null || emailList.isEmpty()) {
            return ResponseEntity.badRequest().body("Email list cannot be empty ❌");
        }

        emailService.sendReminderEmails(emailList);
        return ResponseEntity.ok("Reminder emails sent successfully to " + emailList.size() + " recipients ✅");
    }
}
