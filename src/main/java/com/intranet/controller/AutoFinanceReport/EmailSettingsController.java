package com.intranet.controller.AutoFinanceReport;

import com.intranet.entity.EmailSettings;
import com.intranet.service.cornjobs.AutoFinanceReport.EmailSettingsService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emailSettings")
@CrossOrigin(origins = "*",allowedHeaders = "*")
public class EmailSettingsController {

    private final EmailSettingsService emailSettingsService;

    public EmailSettingsController(EmailSettingsService emailSettingsService) {
        this.emailSettingsService = emailSettingsService;
    }

    @GetMapping
    public List<EmailSettings> getAllEmailSettings() {
        return emailSettingsService.getAllEmailSettings();
    }

    @PutMapping("/{id}")
    public EmailSettings updateEmailSettings(
            @PathVariable Long id,
            @RequestBody String email
    ) {
        return emailSettingsService.updateEmailSettings(id, email);
    }

}
