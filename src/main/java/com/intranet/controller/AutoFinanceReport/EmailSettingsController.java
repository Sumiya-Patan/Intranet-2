package com.intranet.controller.AutoFinanceReport;

import com.intranet.entity.EmailSettings;
import com.intranet.service.cornjobs.AutoFinanceReport.EmailSettingsService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emailSettings")
@CrossOrigin(origins = "*",allowedHeaders = "*")
public class EmailSettingsController {

    private final EmailSettingsService emailSettingsService;

    public EmailSettingsController(EmailSettingsService emailSettingsService) {
        this.emailSettingsService = emailSettingsService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TIMESHEET_ADMIN')")
    public List<EmailSettings> getAllEmailSettings() {
        return emailSettingsService.getAllEmailSettings();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TIMESHEET_ADMIN')")
    public EmailSettings updateEmailSettings(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody
    ) {

        String email = requestBody.get("email");

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email field is required");
        }

        return emailSettingsService.updateEmailSettings(id, email);
    }


}
