package com.intranet.service.email.missingWeekTimesheet;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.intranet.dto.email.MissingTimesheetEmailDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.service.email.ums_corn_job_token.UmsAuthService;
import com.intranet.util.EmailUtil;
import com.intranet.util.cache.UserDirectoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissingTimesheetCronService {

    private final UserDirectoryService userDirectoryService;
    private final TimeSheetRepo timeSheetRepo;
    private final EmailUtil emailUtil;
    private final MissingTimesheetEmailTemplateBuilderService missingTemplateBuilder;
    private final UmsAuthService umsAuthService;

    @Scheduled(cron = "0 0 11 * * MON")
    // @Scheduled(fixedRate = 1000)
    public void checkMissingTimesheetsLastWeek() {

        System.out.println("🔍 CRON STARTED: Checking missing timesheets...");

        // 🔐 Auto-login to UMS
        String token = umsAuthService.getUmsToken();
        String authHeader = "Bearer " + token;

        // ✔ Pass the token to the UserDirectory
        Map<Long, Map<String, Object>> allUsers =
                userDirectoryService.fetchAllUsers(authHeader);

        if (allUsers == null || allUsers.isEmpty()) {
            System.out.println("⚠ No users found.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastWeekEnd = today.minusWeeks(1).with(DayOfWeek.SUNDAY);
        LocalDate lastWeekStart = lastWeekEnd.minusDays(6);

        Set<Long> submittedUsers = timeSheetRepo
                .findByWorkDateBetween(lastWeekStart, lastWeekEnd)
                .stream()
                .map(TimeSheet::getUserId)
                .collect(Collectors.toSet());

        List<Long> missingUsers = allUsers.keySet().stream()
                .filter(id -> !submittedUsers.contains(id))
                .toList();

        for (Long userId : missingUsers) {
            Map<String, Object> user = allUsers.get(userId);
            if (user == null) continue;

            String email = (String) user.get("email");
            String name  = (String) user.get("name");

            if (email == null || email.isBlank()) continue;

            MissingTimesheetEmailDTO dto = new MissingTimesheetEmailDTO();
            dto.setUserName(name);
            dto.setStartDate(lastWeekStart.toString());
            dto.setEndDate(lastWeekEnd.toString());

            String html = missingTemplateBuilder.buildMissingTimesheetEmail(dto);

            try {
                emailUtil.sendEmail(email, "⚠ Weekly Timesheet Missing", html);
                System.out.println("✅ Email sent to: " + email);
            } catch (Exception e) {
                System.out.println("❌ Error sending email: " + e.getMessage());
            }
        }

        System.out.println("📧 Missing users notified.");
    }
}
