package com.intranet.controller.reports;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.report.ManagerMonthlyReportService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class ManagerViewReportController {

    private final ManagerMonthlyReportService managerMonthlyReportService;

    @GetMapping("/managerMonthly")
    public ResponseEntity<?> managerMonthlyReport(
        @CurrentUser UserDTO currentUser,
        @RequestParam(required = false) int month,
        @RequestParam(required = false) int year,
        HttpServletRequest req
    ) {
        
         String token = req.getHeader("Authorization");
        if (token == null){
             token = "";
            throw new IllegalArgumentException("Authorization token is missing");
            };

        try{

            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        // Implementation logic to get manager monthly report
        Map<String, Object> managerMonthlyReport = managerMonthlyReportService.generateManagerMonthlyReport(currentUser.getId(),  startDate, endDate,token);

        return ResponseEntity.ok(managerMonthlyReport);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    
}
