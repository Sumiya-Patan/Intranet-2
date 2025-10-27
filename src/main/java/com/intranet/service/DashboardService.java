package com.intranet.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.repository.TimeSheetRepo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TimeSheetRepo timeSheetRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${pms.api.base-url}")
    private String pmsBaseUrl;

    // --- Helper to get Auth headers from current request ---
    private HttpEntity<Void> buildEntityWithAuth() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return new HttpEntity<>(new HttpHeaders());
        HttpServletRequest request = attrs.getRequest();
        String token = request.getHeader("Authorization");

        HttpHeaders headers = new HttpHeaders();
        if (token != null && !token.isBlank()) headers.set("Authorization", token);
        return new HttpEntity<>(headers);
    }

    public Map<String, Object> getSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        // 0️⃣ Default date range (current month)
        LocalDate today = LocalDate.now();
        if (startDate == null) startDate = today.withDayOfMonth(1);
        if (endDate == null) endDate = today;

        // 1️⃣ Fetch PMS projects
    Map<Long, String> projectMap = new HashMap<>();
    try {
    ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
        pmsBaseUrl + "/projects",
        HttpMethod.GET,
        buildEntityWithAuth(),
        new ParameterizedTypeReference<List<Map<String, Object>>>() {}
    );

    if (resp.getBody() != null) {
        projectMap.putAll(
            resp.getBody().stream()
                .collect(Collectors.toMap(
                        p -> ((Number) p.get("id")).longValue(),
                        p -> (String) p.get("name"),
                        (a, b) -> a
                ))
        );
        }
    } catch (Exception ignored) {
        // Optionally log the exception if desired
    }

    // 2️⃣ Fetch user’s timesheets
    List<TimeSheet> sheets = timeSheetRepo.findByUserIdAndWorkDateBetween(userId, startDate, endDate);

        List<TimeSheetEntry> entries = sheets.stream()
                .flatMap(s -> s.getEntries().stream())
                .toList();

        // 3️⃣ Timesheet status summary
        Map<String, Long> timesheetSummary = Map.of(
                "submitted", sheets.stream().filter(s -> "SUBMITTED".equalsIgnoreCase(s.getStatus().name())).count(),
                "approved", sheets.stream().filter(s -> "APPROVED".equalsIgnoreCase(s.getStatus().name())).count(),
                "rejected", sheets.stream().filter(s -> "REJECTED".equalsIgnoreCase(s.getStatus().name())).count()
        );

        // 4️⃣ Project-wise total hours
        Map<Long, BigDecimal> projectHours = entries.stream()
                .collect(Collectors.groupingBy(
                        TimeSheetEntry::getProjectId,
                        Collectors.mapping(
                                TimeSheetEntry::getHoursWorked,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        List<Map<String, Object>> projectSummary = projectHours.entrySet().stream()
    .map(e -> {
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", e.getKey());
        map.put("projectName", projectMap.getOrDefault(e.getKey(), "Unknown Project"));
        map.put("totalHoursWorked", formatHours(e.getValue()));
        return map;
    })
    .collect(Collectors.toList());


        // 5️⃣ Weekly (Day-wise) summary
        Map<String, BigDecimal> weeklySummary = Arrays.stream(DayOfWeek.values())
                .collect(Collectors.toMap(
                        d -> d.name().toLowerCase(),
                        d -> BigDecimal.ZERO,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        for (TimeSheetEntry entry : entries) {
            LocalDate date = entry.getTimeSheet().getWorkDate();
            BigDecimal hours = entry.getHoursWorked();
            weeklySummary.merge(date.getDayOfWeek().name().toLowerCase(), hours, BigDecimal::add);
        }

        weeklySummary.replaceAll((k, v) -> formatHours(v));

        // 6️⃣ Total hours worked
        BigDecimal totalHours = entries.stream()
                .map(TimeSheetEntry::getHoursWorked)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalHours = formatHours(totalHours);

        // 7️⃣ Aggregate stats
        long totalTasks = entries.stream()
                .map(TimeSheetEntry::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long totalDays = sheets.stream()
                .map(TimeSheet::getWorkDate)
                .distinct()
                .count();

        String avgHoursPerDay = "00:00";
        if (totalDays > 0 && totalHours.compareTo(BigDecimal.ZERO) > 0) {
            long totalMinutes = totalHoursToMinutes(totalHours);
            long avgMinutes = totalMinutes / totalDays;
            avgHoursPerDay = String.format("%02d:%02d", avgMinutes / 60, avgMinutes % 60);
        }

        return Map.of(
                "timesheetSummary", timesheetSummary,
                "projectSummary", projectSummary,
                "weeklySummary", weeklySummary,
                "totalHours", totalHours,
                "totalTasks", totalTasks,
                "averageHoursPerDay", avgHoursPerDay,
                "dateRange", Map.of("startDate", startDate, "endDate", endDate)
        );
    }

    // --- Helpers ---
    private BigDecimal formatHours(BigDecimal value) {
        String str = value.toString();
        String[] parts = str.split("\\.");
        int hours = Integer.parseInt(parts[0]);
        int mins = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        if (mins >= 60) {
            hours += mins / 60;
            mins %= 60;
        }
        return new BigDecimal(String.format("%02d.%02d", hours, mins));
    }

    private long totalHoursToMinutes(BigDecimal hhmm) {
        String[] parts = hhmm.toString().split("\\.");
        int hours = Integer.parseInt(parts[0]);
        int mins = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return (hours * 60L) + mins;
    }
    
}
