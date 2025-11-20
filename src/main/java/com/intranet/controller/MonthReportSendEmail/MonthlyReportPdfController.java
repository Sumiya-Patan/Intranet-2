package com.intranet.controller.MonthReportSendEmail;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.intranet.dto.MonthlyUserReportDTO;
import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.MonthlyUserReportService;
import com.intranet.service.MonthReportEmailSend.EmailPdfSenderService;
import com.intranet.service.MonthReportEmailSend.PdfGeneratorService;
import com.intranet.service.MonthReportEmailSend.PdfTemplateBuilder;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class MonthlyReportPdfController {

    private final PdfTemplateBuilder templateBuilder;
    private final PdfGeneratorService pdfGenerator;
    private final EmailPdfSenderService emailSender;
    private final MonthlyUserReportService reportService;


    @Value("${ums.api.base-url}")
    private String umsBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private HttpEntity<Void> buildEntityWithAuth() {

    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null) {
        return (HttpEntity<Void>) HttpEntity.EMPTY;
    }

    HttpServletRequest request = attrs.getRequest();
    String authHeader = request.getHeader("Authorization");

    HttpHeaders headers = new HttpHeaders();
    if (authHeader != null && !authHeader.isBlank()) {
        headers.set("Authorization", authHeader);
    }

    return new HttpEntity<>(headers);
    }

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


         // 2️⃣ Extract Authorization Header
            HttpEntity<Void> entity = buildEntityWithAuth();
            String authHeader = entity.getHeaders().getFirst("Authorization");

            if (authHeader == null) {
                return ResponseEntity.badRequest().body("Missing Authorization header.");
            }
        
            String userEmail = fetchUserEmail(currentUser.getId(), authHeader);

        if (userEmail == null) {
            return ResponseEntity.badRequest().body("User email not found in UMS.");
        }

        // 3️⃣ Convert to HTML
        String html = templateBuilder.buildUserMonthlyReportHtml(reportDTO);

        // 4️⃣ Convert to PDF
        byte[] pdfBytes = pdfGenerator.generatePdfFromHtml(html);

        // 5️⃣ Email the report
        emailSender.sendPdfReport(userEmail, pdfBytes, reportDTO.getEmployeeName());

        return ResponseEntity.ok("PDF generated and sent to " + userEmail);

    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
    }
    public String fetchUserEmail(Long userId, String authHeader) {

    String url = umsBaseUrl + "/admin/users/" + userId;

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", authHeader);

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {}
    );

    Map<String, Object> body = response.getBody();
    if (body == null) return null;

    return (String) body.get("mail");
    }

}   
