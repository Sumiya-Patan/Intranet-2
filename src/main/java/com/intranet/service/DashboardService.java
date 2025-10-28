package com.intranet.service;

import com.intranet.entity.*;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.WeeklyTimeSheetReviewRepo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TimeSheetRepo timeSheetRepo;
    private final WeeklyTimeSheetReviewRepo weeklyReviewRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${pms.api.base-url}")
    private String pmsBaseUrl;

    // 🧩 Build Auth Headers
    private HttpEntity<Void> buildEntityWithAuth() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpHeaders headers = new HttpHeaders();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String token = request.getHeader("Authorization");
            if (token != null && !token.isBlank()) headers.set("Authorization", token);
        }
        return new HttpEntity<>(headers);
    }

    // 🧾 MAIN SERVICE METHOD
    public Map<String, Object> getDashboardSummary(Long userId, LocalDate startDate, LocalDate endDate) {

        LocalDate today = LocalDate.now();
        if (startDate == null) startDate = today.withDayOfMonth(1);
        if (endDate == null) endDate = today;

        // 1️⃣ Fetch PMS Project Names
        Map<Long, String> projectMap = fetchProjectMap();

        // 2️⃣ Fetch Timesheets
        List<TimeSheet> timeSheets = timeSheetRepo.findByUserIdAndWorkDateBetween(userId, startDate, endDate);
        List<TimeSheetEntry> entries = timeSheets.stream()
                .flatMap(ts -> ts.getEntries().stream())
                .collect(Collectors.toList());

        // 3️⃣ Weekly Review Summary using WeekInfo + WeekTimeSheetReview
        Map<String, Object> weeklyTimesheetReview = getWeeklySummary(userId, startDate, endDate);

        // 4️⃣ Billable Activity
        Map<String, Object> billableActivity = calculateBillableActivity(entries);

        // 5️⃣ Project Summary
        List<Map<String, Object>> projectSummary = calculateProjectSummary(entries, projectMap);

        // 6️⃣ Weekly Summary (Mon–Sun)
        Map<String, BigDecimal> weeklySummary = calculateWeeklySummary(entries);

        // 7️⃣ Productivity Details
        Map<String, Object> productivityDetails = calculateProductivityDetails(entries);

        // 8️⃣ Total Hours + Avg Hours per Day
        BigDecimal totalHours = entries.stream()
                .map(TimeSheetEntry::getHoursWorked)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalHours = formatHours(totalHours);

        long totalDays = timeSheets.stream()
                .map(TimeSheet::getWorkDate)
                .distinct()
                .count();

        String avgHoursPerDay = calculateAverageHoursPerDay(totalHours, totalDays);

        // 🧾 Final Aggregated Response
        return Map.of(
                "weeklyTimesheetReview", weeklyTimesheetReview,
                "billableActivity", billableActivity,
                "projectSummary", projectSummary,
                "weeklySummary", weeklySummary,
                "productivityDetails", productivityDetails,
                "totalHours", totalHours,
                "averageHoursPerDay", avgHoursPerDay,
                "dateRange", Map.of("startDate", startDate, "endDate", endDate)
        );
    }

    // ✅ Fetch Project Map
    private Map<Long, String> fetchProjectMap() {
        try {
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    pmsBaseUrl + "/projects",
                    HttpMethod.GET,
                    buildEntityWithAuth(),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            if (resp.getBody() == null) return new HashMap<>();

            return resp.getBody().stream()
                    .collect(Collectors.toMap(
                            p -> ((Number) p.get("id")).longValue(),
                            p -> (String) p.get("name"),
                            (a, b) -> a
                    ));
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    // ✅ Billable Activity
    private Map<String, Object> calculateBillableActivity(List<TimeSheetEntry> entries) {
        long billableTasks = entries.stream().filter(TimeSheetEntry::isBillable).count();
        long nonBillableTasks = entries.size() - billableTasks;
        double billablePercentage = entries.isEmpty() ? 0.0 :
                (billableTasks * 100.0) / entries.size();

        return Map.of(
                "billableTasks", billableTasks,
                "nonBillableTasks", nonBillableTasks,
                "billablePercentage", round(billablePercentage, 1)
        );
    }

    // ✅ Project Summary
    private List<Map<String, Object>> calculateProjectSummary(List<TimeSheetEntry> entries, Map<Long, String> projectMap) {
        Map<Long, BigDecimal> projectHours = entries.stream()
                .collect(Collectors.groupingBy(
                        TimeSheetEntry::getProjectId,
                        Collectors.mapping(TimeSheetEntry::getHoursWorked,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        return projectHours.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("projectId", e.getKey());
            m.put("projectName", projectMap.getOrDefault(e.getKey(), "Unknown Project"));
            m.put("totalHoursWorked", formatHours(e.getValue()));
            return m;
        }).collect(Collectors.toList());
    }

    // ✅ Weekly Summary (hours by weekday)
    private Map<String, BigDecimal> calculateWeeklySummary(List<TimeSheetEntry> entries) {
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
        return weeklySummary;
    }

    // ✅ Productivity Details (by day)
    private Map<String, Object> calculateProductivityDetails(List<TimeSheetEntry> entries) {
        Map<String, List<TimeSheetEntry>> dayWise = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getTimeSheet().getWorkDate().getDayOfWeek().name().toLowerCase()));

        Map<String, Object> productivity = new LinkedHashMap<>();

        for (DayOfWeek day : DayOfWeek.values()) {
            List<TimeSheetEntry> dayEntries = dayWise.getOrDefault(day.name().toLowerCase(), new ArrayList<>());

            BigDecimal totalHours = dayEntries.stream()
                    .map(TimeSheetEntry::getHoursWorked)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long billableCount = dayEntries.stream().filter(TimeSheetEntry::isBillable).count();
            long totalCount = dayEntries.size();
            double billablePercentage = totalCount == 0 ? 0.0 : (billableCount * 100.0 / totalCount);

            long tasksWorked = dayEntries.stream().map(TimeSheetEntry::getTaskId).filter(Objects::nonNull).distinct().count();
            long projectsWorked = dayEntries.stream().map(TimeSheetEntry::getProjectId).filter(Objects::nonNull).distinct().count();

            double productivityScore = Math.min(100.0, billablePercentage + (tasksWorked * 2) + (projectsWorked * 3));

            productivity.put(day.name().toLowerCase(), Map.of(
                    "totalHours", formatHours(totalHours),
                    "billablePercentage", round(billablePercentage, 1),
                    "tasksWorked", tasksWorked,
                    "projectsWorked", projectsWorked,
                    "productivityScore", round(productivityScore, 1)
            ));
        }

        return productivity;
    }

    // ✅ Week-based Review Summary (using WeekInfo & WeekTimeSheetReview)
    public Map<String, Object> getWeeklySummary(Long userId, LocalDate startDate, LocalDate endDate) {

        List<WeeklyTimeSheetReview> reviews =
                weeklyReviewRepo.findByUserIdAndWeekInfo_StartDateBetween(userId, startDate, endDate);

        if (reviews.isEmpty()) {
            return Map.of(
                    "submittedWeeks", 0L,
                    "approvedWeeks", 0L,
                    "rejectedWeeks", 0L,
                    "weekDetails", List.of()
            );
        }

        Map<WeekInfo, List<WeeklyTimeSheetReview>> grouped = reviews.stream()
                .collect(Collectors.groupingBy(WeeklyTimeSheetReview::getWeekInfo));

        List<Map<String, Object>> weekDetails = new ArrayList<>();
        long submitted = 0, approved = 0, rejected = 0;

        for (var entry : grouped.entrySet()) {
            WeekInfo week = entry.getKey();
            List<String> statuses = entry.getValue().stream()
                    .map(r -> r.getStatus().name())
                    .toList();

            String finalStatus;
            if (statuses.contains("REJECTED")) {
                finalStatus = "REJECTED"; rejected++;
            } else if (statuses.stream().allMatch(s -> s.equals("APPROVED"))) {
                finalStatus = "APPROVED"; approved++;
            } else {
                finalStatus = "SUBMITTED"; submitted++;
            }

            weekDetails.add(Map.of(
                    "weekNumber", week.getWeekNo(),
                    "year", week.getYear(),
                    "startDate", week.getStartDate(),
                    "endDate", week.getEndDate(),
                    "status", finalStatus
            ));
        }

        return Map.of(
                "submittedWeeks", submitted,
                "approvedWeeks", approved,
                "rejectedWeeks", rejected,
                "weekDetails", weekDetails
        );
    }

    // ✅ Utility Helpers
    private BigDecimal formatHours(BigDecimal value) {
        String str = value.toPlainString();
        String[] parts = str.split("\\.");
        int hours = Integer.parseInt(parts[0]);
        int mins = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        if (mins >= 60) {
            hours += mins / 60;
            mins %= 60;
        }
        return new BigDecimal(String.format("%02d.%02d", hours, mins));
    }

    private String calculateAverageHoursPerDay(BigDecimal totalHours, long totalDays) {
        if (totalDays == 0 || totalHours.compareTo(BigDecimal.ZERO) <= 0) return "00:00";
        String[] parts = totalHours.toPlainString().split("\\.");
        int hours = Integer.parseInt(parts[0]);
        int mins = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        long totalMinutes = (hours * 60L) + mins;
        long avgMinutes = totalMinutes / totalDays;
        return String.format("%02d:%02d", avgMinutes / 60, avgMinutes % 60);
    }

    private double round(double value, int places) {
        return Math.round(value * Math.pow(10, places)) / Math.pow(10, places);
    }
}
