package com.intranet.controller.external;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.intranet.dto.UserDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.entity.TimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.TimeSheetReviewRepo;
import com.intranet.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api")
public class ManagerSummaryController {

    @Value("${pms.api.base-url}")
    private String pmsBaseUrl;

    @Autowired
    private TimeSheetRepo timeSheetRepository;

    @Autowired
    private TimeSheetReviewRepo reviewRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Operation(summary = "Get summary of timesheets for a manager's team within a date range (includes weekly breakdown)")
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

        // 🔐 Forward Authorization header
        String authHeader = request.getHeader("Authorization");
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isBlank()) headers.set("Authorization", authHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 🔗 Fetch manager’s projects
        String url = String.format("%s/projects/owner", pmsBaseUrl);
        ResponseEntity<List<Map<String, Object>>> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
        List<Map<String, Object>> projects = response.getBody();

        if (projects == null || projects.isEmpty()) {
            return ResponseEntity.ok(emptySummary(startDate, endDate));
        }

        // 👥 Collect team member IDs
        Set<Long> memberIds = projects.stream()
                .flatMap(p -> Optional.ofNullable((List<Map<String, Object>>) p.get("members"))
                        .orElse(Collections.emptyList()).stream())
                .map(m -> ((Number) m.get("id")).longValue())
                .collect(Collectors.toSet());

        if (memberIds.isEmpty()) {
            return ResponseEntity.ok(emptySummary(startDate, endDate));
        }

        // 🗓️ Fetch all timesheets in range
        List<TimeSheet> teamSheets = timeSheetRepository
                .findByUserIdInAndWorkDateBetween(memberIds, startDate, endDate);

        if (teamSheets.isEmpty()) {
            return ResponseEntity.ok(emptySummary(startDate, endDate));
        }

        // 🧮 Flatten all entries
        List<TimeSheetEntry> allEntries = teamSheets.stream()
                .flatMap(ts -> ts.getEntries().stream())
                .toList();

        // 📊 Total & Billable hours
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

        // ⏳ Count pending timesheets
        long pendingCount = teamSheets.stream()
                .filter(ts -> {
                    Optional<TimeSheetReview> reviewOpt = reviewRepository.findByTimeSheetAndManagerId(ts, user.getId());
                    return reviewOpt.isEmpty() || "Pending".equalsIgnoreCase(reviewOpt.get().getAction());
                })
                .count();

        // 🧮 Fix weekly summary accumulation
    Map<String, BigDecimal> weeklySummary = new LinkedHashMap<>();

    for (DayOfWeek day : DayOfWeek.values()) {
        if (day == DayOfWeek.SUNDAY) continue; // skip Sunday

    long totalMinutes = teamSheets.stream()
            .filter(ts -> ts.getWorkDate().getDayOfWeek() == day)
            .flatMap(ts -> ts.getEntries().stream())
            .mapToLong(e -> {
                if (e.getFromTime() != null && e.getToTime() != null) {
                    return Duration.between(e.getFromTime(), e.getToTime()).toMinutes();
                }
                return 0L;
            })
            .sum();

    // ✅ Convert to proper HH.mm
    long hours = totalMinutes / 60;
    long minutes = totalMinutes % 60;
    String formatted = String.format("%d.%02d", hours, minutes);

    weeklySummary.put(day.name(), new BigDecimal(formatted));
    }


        // 🧾 Final Response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalHours", totalHours);
        result.put("billableHours", billableHours);
        result.put("billablePercentage", billablePercentage);
        result.put("pending", pendingCount);
        result.put("dateRange", Map.of("startDate", startDate, "endDate", endDate));
        result.put("weeklySummary", weeklySummary);

        return ResponseEntity.ok(result);
    }

    // 🧩 Empty response helper
    private Map<String, Object> emptySummary(LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> emptyWeek = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day != DayOfWeek.SUNDAY) emptyWeek.put(day.name(), BigDecimal.ZERO);
        }
        return Map.of(
                "totalHours", BigDecimal.ZERO,
                "billableHours", BigDecimal.ZERO,
                "billablePercentage", 0.0,
                "pending", 0,
                "dateRange", Map.of("startDate", startDate, "endDate", endDate),
                "weeklySummary", emptyWeek
        );
    }

    // ⏱️ Duration calculation helper
    private BigDecimal calculateHours(TimeSheetEntry entry) {
    if (entry == null || entry.getFromTime() == null || entry.getToTime() == null) {
        return BigDecimal.ZERO;
    }

    Duration duration = Duration.between(entry.getFromTime(), entry.getToTime());
    long totalMinutes = duration.toMinutes();
    long hours = totalMinutes / 60;
    long minutes = totalMinutes % 60;

    // ✅ Correctly represent as HH.mm (not 5.70 but 6.10)
    String formatted = String.format("%d.%02d", hours, minutes);
    return new BigDecimal(formatted);
    }

        }
