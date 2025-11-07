package com.intranet.controller.external;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import com.intranet.entity.TimeSheetReview;
import com.intranet.entity.WeeklyTimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.TimeSheetReviewRepo;
import com.intranet.repository.WeeklyTimeSheetReviewRepo;
import com.intranet.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api")
public class ManagerSummaryController {

    @Value("${pms.api.base-url}")
    private String pmsBaseUrl;

    @Value("${ums.api.base-url}")
    private String umsBaseUrl;

    @Autowired
    private TimeSheetRepo timeSheetRepository;

    @Autowired
    private TimeSheetReviewRepo timeSheetReviewRepo;

    @Autowired
    private WeeklyTimeSheetReviewRepo weeklyReviewRepo;

    private final RestTemplate restTemplate = new RestTemplate();

    @Operation(summary = "Manager dashboard summary for the team")
    @GetMapping("/manager/summary")
    @PreAuthorize("hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<Map<String, Object>> getTeamSummary(
            @CurrentUser UserDTO user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        LocalDate today = LocalDate.now();
        if (startDate == null) startDate = today.withDayOfMonth(1);
        if (endDate == null) endDate = today.with(TemporalAdjusters.lastDayOfMonth());


        // Forward Authorization header
        String authHeader = request.getHeader("Authorization");
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set("Authorization", authHeader);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Fetch manager’s projects
        String url = String.format("%s/projects/owner", pmsBaseUrl);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        List<Map<String, Object>> projects = response.getBody();

        if (projects == null || projects.isEmpty()) {
            return ResponseEntity.ok(emptySummary(startDate, endDate));
        }

        // Collect team member IDs
        Set<Long> memberIds = projects.stream()
                .flatMap(p -> Optional.ofNullable((List<Map<String, Object>>) p.get("members"))
                        .orElse(Collections.emptyList()).stream())
                .map(m -> ((Number) m.get("id")).longValue())
                .collect(Collectors.toSet());

        if (memberIds.isEmpty()) {
            return ResponseEntity.ok(emptySummary(startDate, endDate));
        }

        // --- Step: Build UMS user cache (names + emails) ---
        Map<Long, String> userCacheFullName = new HashMap<>();
        Map<Long, String> userCacheEmail = new HashMap<>();

        try {
            String umsUrl = String.format("%s/admin/users?page=1&limit=500", umsBaseUrl);

            ResponseEntity<Map<String, Object>> umsResponse = restTemplate.exchange(
                    umsUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> umsBody = umsResponse.getBody();

            if (umsBody != null && umsBody.containsKey("users")) {
                Object usersObj = umsBody.get("users");

                if (usersObj instanceof List<?>) {
                    List<?> userList = (List<?>) usersObj;

                    for (Object obj : userList) {
                        if (!(obj instanceof Map)) continue;

                        @SuppressWarnings("unchecked")
                        Map<String, Object> userMap = (Map<String, Object>) obj;

                        Number idNum = (Number) userMap.get("user_id");
                        if (idNum == null) continue;

                        Long userId = idNum.longValue();
                        String firstName = (String) userMap.getOrDefault("first_name", "");
                        String lastName = (String) userMap.getOrDefault("last_name", "");
                        String email = (String) userMap.getOrDefault("mail", "unknown@example.com");

                        String fullName = (firstName + " " + lastName).trim();

                        userCacheFullName.put(userId, fullName.isEmpty() ? "Unknown" : fullName);
                        userCacheEmail.put(userId, email);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to load users from UMS: " + e.getMessage());
        }

        // ✅ Fallback for any missing users
        for (Long userId : memberIds) {
            userCacheFullName.putIfAbsent(userId, "Unknown");
            userCacheEmail.putIfAbsent(userId, "unknown@example.com");
        }

        // Fetch all timesheets
        List<TimeSheet> teamSheets = timeSheetRepository
                .findByUserIdInAndWorkDateBetween(memberIds, startDate, endDate);

        List<TimeSheetEntry> allEntries = teamSheets.stream()
                .flatMap(ts -> ts.getEntries().stream())
                .collect(Collectors.toList());

        // Total and billable hours
        BigDecimal totalHours = allEntries.stream()
                .map(this::calculateHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal billableHours = allEntries.stream()
                .filter(TimeSheetEntry::isBillable)
                .map(this::calculateHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double billablePercentage = totalHours.compareTo(BigDecimal.ZERO) > 0
                ? billableHours.divide(totalHours, 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

       
        // ✅ STEP 1: Get all SUBMITTED or PARTIALLY_APPROVED sheets
        List<TimeSheet> relevantSheets = teamSheets.stream()
                .filter(ts -> {
                String status = ts.getStatus().name();
                return "SUBMITTED".equalsIgnoreCase(status)
                        || "PARTIALLY_APPROVED".equalsIgnoreCase(status);
                })
                .collect(Collectors.toList());

        int pendingCount = 0;
        List<Map<String, Object>> pendingUsers = Collections.emptyList();

        if (!relevantSheets.isEmpty()) {

        // ✅ STEP 2: Extract all weekIds
        Set<Long> weekIds = relevantSheets.stream()
                .map(ts -> ts.getWeekInfo().getId())
                .collect(Collectors.toSet());

        // ✅ STEP 3: Get all reviews by this manager for these weeks
        List<TimeSheetReview> managerReviews =
                timeSheetReviewRepo.findByManagerIdAndWeekInfo_IdIn(user.getId(), weekIds);

        // ✅ STEP 4: Build lookup for reviewed pairs (user-week)
        Set<String> reviewedKeys = managerReviews.stream()
                .filter(r -> {
                        String s = r.getStatus().name();
                        return "APPROVED".equalsIgnoreCase(s) || "REJECTED".equalsIgnoreCase(s);
                })
                .map(r -> r.getUserId() + "-" + r.getWeekInfo().getId())
                .collect(Collectors.toSet());

        // ✅ STEP 5: Identify pending (no review or not approved/rejected)
        Map<Long, Set<Long>> pendingWeeksByUser = relevantSheets.stream()
                .filter(ts -> {
                        String key = ts.getUserId() + "-" + ts.getWeekInfo().getId();
                        return !reviewedKeys.contains(key); // Not yet reviewed
                })
                .collect(Collectors.groupingBy(
                        TimeSheet::getUserId,
                        Collectors.mapping(ts -> ts.getWeekInfo().getId(), Collectors.toSet())
                ));

        // ✅ STEP 6: Build per-user summary
        pendingUsers = pendingWeeksByUser.entrySet().stream()
                .map(entry -> {
                        Long userId = entry.getKey();
                        int weekCount = entry.getValue().size();
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("userId", userId);
                        userMap.put("userName", userCacheFullName.getOrDefault(userId, "Unknown"));
                        userMap.put("pendingWeeks", weekCount);
                        return userMap;
                })
                .collect(Collectors.toList());

        // ✅ STEP 7: Total pending count (sum of all user-week pairs)
        pendingCount = pendingWeeksByUser.values().stream()
                .mapToInt(Set::size)
                .sum();

        pendingUsers.forEach(u -> System.out.println("   → " + u));
                }




        // Missing timesheets
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate fromDate = today.getDayOfMonth() < 15 ? monthStart : today.minusDays(15);

        List<WeeklyTimeSheetReview> recentSubmissions = weeklyReviewRepo
                .findByUserIdInAndWeekInfo_StartDateBetween(memberIds, fromDate, today);

        Set<Long> activeUsers = recentSubmissions.stream()
                .map(WeeklyTimeSheetReview::getUserId)
                .collect(Collectors.toSet());

        List<Map<String, Object>> missingUsers = memberIds.stream()
                .filter(id -> !activeUsers.contains(id))
                .map(id -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("userId", id);
                    userMap.put("fullName", userCacheFullName.getOrDefault(id, "Unknown"));
                    userMap.put("email", userCacheEmail.getOrDefault(id, "unknown@example.com"));
                    return userMap;
                })
                .collect(Collectors.toList());

        // Weekly summary
        Map<String, BigDecimal> weeklySummary = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SUNDAY) continue;
            long totalMinutes = teamSheets.stream()
                    .filter(ts -> ts.getWorkDate().getDayOfWeek() == day)
                    .flatMap(ts -> ts.getEntries().stream())
                    .mapToLong(e -> e.getFromTime() != null && e.getToTime() != null
                            ? Duration.between(e.getFromTime(), e.getToTime()).toMinutes()
                            : 0L)
                    .sum();
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            weeklySummary.put(day.name(), new BigDecimal(String.format("%d.%02d", hours, minutes)));
        }

        // Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalHours", totalHours);
        result.put("billableHours", billableHours);
        result.put("billablePercentage", billablePercentage);
        result.put("pending", pendingCount);
        result.put("pendingUsers", pendingUsers);
        result.put("dateRange", Map.of("startDate", startDate, "endDate", endDate));
        result.put("weeklySummary", weeklySummary);
        result.put("missingTimesheets", missingUsers);

        return ResponseEntity.ok(result);
    }

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
                "weeklySummary", emptyWeek,
                "missingTimesheets", Collections.emptyList()
        );
    }

    private BigDecimal calculateHours(TimeSheetEntry entry) {
        if (entry == null || entry.getFromTime() == null || entry.getToTime() == null)
            return BigDecimal.ZERO;
        Duration duration = Duration.between(entry.getFromTime(), entry.getToTime());
        long totalMinutes = duration.toMinutes();
        return new BigDecimal(String.format("%d.%02d", totalMinutes / 60, totalMinutes % 60));
    }
}
