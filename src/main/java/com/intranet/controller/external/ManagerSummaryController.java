package com.intranet.controller.external;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.intranet.dto.UserDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import java.util.*;


@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api")
public class ManagerSummaryController {

    @Value("${pms.api.base-url}")
    private String pmsBaseUrl;

    @Autowired
    private TimeSheetRepo timeSheetRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Operation(summary = "Get summary of timesheets for a manager's team within a date range")
    @GetMapping("/manager/summary")
    @PreAuthorize("hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<Map<String, Object>> getTeamSummary(
            @CurrentUser UserDTO user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        LocalDate today = LocalDate.now();
        if (startDate == null) startDate = today.withDayOfMonth(1);
        if (endDate == null) endDate = today;

        // Forward authorization
        String authHeader = request.getHeader("Authorization");
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isBlank()) headers.set("Authorization", authHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Fetch manager's projects
        String url = String.format("%s/projects/owner", pmsBaseUrl);
        ResponseEntity<List<Map<String, Object>>> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
        List<Map<String, Object>> projects = response.getBody();

        if (projects == null || projects.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "totalHours", BigDecimal.ZERO,
                    "billableHours", BigDecimal.ZERO,
                    "billablePercentage", 0.0,
                    "dateRange", Map.of("startDate", startDate, "endDate", endDate)
            ));
        }

        // Collect member IDs
        Set<Long> memberIds = projects.stream()
                .flatMap(p -> Optional.ofNullable((List<Map<String, Object>>) p.get("members")).orElse(Collections.emptyList()).stream())
                .map(m -> ((Number) m.get("id")).longValue())
                .collect(Collectors.toSet());

        if (memberIds.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "totalHours", BigDecimal.ZERO,
                    "billableHours", BigDecimal.ZERO,
                    "billablePercentage", 0.0,
                    "dateRange", Map.of("startDate", startDate, "endDate", endDate)
            ));
        }

        // Fetch timesheets in date range
        List<TimeSheet> teamSheets = timeSheetRepository.findByUserIdInAndWorkDateBetween(memberIds, startDate, endDate);

        List<TimeSheetEntry> allEntries = teamSheets.stream()
                .flatMap(s -> s.getEntries().stream())
                .toList();

        BigDecimal totalHours = allEntries.stream()
                .map(this::calculateHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal billableHours = allEntries.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsBillable()))
                .map(this::calculateHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double billablePercentage = 0.0;
        if (totalHours.compareTo(BigDecimal.ZERO) > 0) {
            billablePercentage = billableHours
                    .divide(totalHours, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        Map<String, Object> result = Map.of(
                "totalHours", totalHours,
                "billableHours", billableHours,
                "billablePercentage", billablePercentage,
                "dateRange", Map.of("startDate", startDate, "endDate", endDate)
        );

        return ResponseEntity.ok(result);
    }

    // Calculates hours in decimal (HH.mm)
    private BigDecimal calculateHours(TimeSheetEntry entry) {
        if (entry == null || entry.getFromTime() == null || entry.getToTime() == null) {
            return BigDecimal.ZERO;
        }
        Duration duration = Duration.between(entry.getFromTime(), entry.getToTime());
        long minutes = duration.toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal formatHours(BigDecimal hoursDecimal) {
        long totalMinutes = hoursDecimal.multiply(BigDecimal.valueOf(60)).longValue();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return new BigDecimal(String.format("%d.%02d", hours, minutes));
    }
}
