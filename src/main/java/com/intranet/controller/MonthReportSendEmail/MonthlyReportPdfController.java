package com.intranet.controller.MonthReportSendEmail;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.intranet.dto.MonthlyUserReportDTO;
import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.MonthlyUserReportService;
import com.intranet.service.MonthReportEmailSend.EmailPdfSenderService;
import com.intranet.service.MonthReportEmailSend.PdfGeneratorService;
import com.intranet.service.MonthReportEmailSend.PdfTemplateBuilder;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class MonthlyReportPdfController {

    private final PdfTemplateBuilder templateBuilder;
    private final PdfGeneratorService pdfGenerator;
    private final EmailPdfSenderService emailSender;
    private final MonthlyUserReportService reportService;

    @GetMapping("/userMonthlyPdf")
    @Operation(summary = "Generate Monthly Report PDF for the current user and send via email")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<?> generatePdf(
        @CurrentUser UserDTO currentUser,
        @RequestParam int month,
        @RequestParam int year) {

    try {

        // 1️⃣ Fetch report
        MonthlyUserReportDTO reportDTO =
                reportService.getMonthlyUserReport(currentUser.getId(), month, year);
    
        String monthName = java.time.Month.of(month)
        .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);

        String userEmail = currentUser.getEmail();

        // 3️⃣ Convert to HTML
        String html = templateBuilder.buildUserMonthlyReportHtml(reportDTO, monthName, year);

        // 4️⃣ Convert to PDF
        byte[] pdfBytes = pdfGenerator.generatePdfFromHtml(html);

        // 5️⃣ Email the report
        emailSender.sendPdfReport(userEmail, pdfBytes, reportDTO.getEmployeeName());

        return ResponseEntity.ok("Report generated and sent to " + userEmail);

    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
    }

}   
