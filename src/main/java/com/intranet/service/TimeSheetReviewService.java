package com.intranet.service;

import com.intranet.dto.TimeSheetBulkReviewRequestDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetReview;
import com.intranet.entity.WeekInfo;
import com.intranet.entity.WeeklyTimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.TimeSheetReviewRepo;
import com.intranet.repository.WeekInfoRepo;
import com.intranet.repository.WeeklyTimeSheetReviewRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeSheetReviewService {

    private final TimeSheetRepo timeSheetRepo;
    private final TimeSheetReviewRepo reviewRepo;
    private final WeeklyTimeSheetReviewRepo weeklyReviewRepo;
    private final WeekInfoRepo weekInfoRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${pms.api.base-url}")
    private String pmsBaseUrl;

    /** Utility — reuse same approach from WeeklySummaryService */
    private HttpEntity<Void> buildEntityWithAuth() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return new HttpEntity<>(new HttpHeaders());
        HttpServletRequest request = attrs.getRequest();

        String authHeader = request.getHeader("Authorization");
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set("Authorization", authHeader);
        }
        return new HttpEntity<>(headers);
    }

    @Transactional
    public String reviewMultipleTimesheets(Long managerId, TimeSheetBulkReviewRequestDTO dto) {
        if (dto.getTimesheetIds() == null || dto.getTimesheetIds().isEmpty()) {
            throw new IllegalArgumentException("Timesheet IDs must be provided.");
        }

        List<TimeSheet> sheets = timeSheetRepo.findAllById(dto.getTimesheetIds());
        if (sheets.isEmpty()) {
            throw new IllegalArgumentException("No timesheets found for given IDs.");
        }

        Long firstWeekId = sheets.get(0).getWeekInfo().getId();
        boolean allSameWeek = sheets.stream()
                .allMatch(ts -> ts.getWeekInfo().getId().equals(firstWeekId));

        if (!allSameWeek) {
            throw new IllegalArgumentException("All timesheets must belong to the same week.");
        }

        boolean allSubmitted = sheets.stream()
                .allMatch(ts -> ts.getStatus() != TimeSheet.Status.DRAFT);

        if (!allSubmitted) {
            throw new IllegalArgumentException("Only 'Submitted' timesheets can be reviewed.");
        }

        String status = dto.getStatus().toUpperCase();
        if (!status.equals("APPROVED") && !status.equals("REJECTED")) {
            throw new IllegalArgumentException("Invalid status. Must be APPROVED or REJECTED.");
        }

        if ("REJECTED".equalsIgnoreCase(dto.getStatus()) &&
                (dto.getComments() == null || dto.getComments().isBlank())) {
            throw new IllegalArgumentException("Comments are required when rejecting a timesheet.");
        }

        WeekInfo weekInfo = sheets.get(0).getWeekInfo();
        Long userId = dto.getUserId();

        if (weekInfo.getEndDate().isBefore(LocalDate.now().minusDays(30))) {
            throw new IllegalArgumentException("Cannot review timesheets older than 30 days.");
        }

        // ✅ Step 1: Fetch all projects once (to get managers)
        List<Map<String, Object>> managerProjects = fetchAllProjects();

        for (TimeSheet ts : sheets) {
            TimeSheetReview review = reviewRepo
                    .findByTimeSheet_IdAndManagerId(ts.getId(), managerId)
                    .orElseGet(TimeSheetReview::new);

            review.setUserId(userId);
            review.setManagerId(managerId);
            review.setTimeSheet(ts);
            review.setWeekInfo(weekInfo);
            review.setStatus(TimeSheetReview.Status.valueOf(dto.getStatus().toUpperCase()));
            review.setComments(dto.getComments());
            review.setReviewedAt(LocalDateTime.now());
            reviewRepo.save(review);

            // ✅ Step 2: Recalculate correct overall timesheet status (considering all managers)
            TimeSheet.Status overallStatus = calculateOverallStatus(ts, managerProjects);
            ts.setStatus(overallStatus);
            timeSheetRepo.save(ts);
        }

        // ✅ Step 3: Update weekly review aggregate
        updateWeeklyTimeSheetReview(userId, firstWeekId);

        return String.format("%d timesheets %s successfully.",
                sheets.size(), dto.getStatus().toUpperCase());
    }

    /** Fetch all PMS projects to identify each project's owner (manager) */
    private List<Map<String, Object>> fetchAllProjects() {
        try {
            String projectsUrl = String.format("%s/projects", pmsBaseUrl);
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    projectsUrl,
                    HttpMethod.GET,
                    buildEntityWithAuth(),
                    new ParameterizedTypeReference<>() {}
            );
            return Optional.ofNullable(response.getBody()).orElse(Collections.emptyList());
        } catch (Exception e) {
            System.err.println("⚠️ Failed to fetch PMS projects: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /** ✅ Corrected logic — checks all managers who SHOULD review, not only existing reviews */
    private TimeSheet.Status calculateOverallStatus(TimeSheet ts, List<Map<String, Object>> managerProjects) {
        // 1️⃣ Determine all required manager IDs based on project owners
        Set<Long> requiredManagerIds = ts.getEntries().stream()
                .map(entry -> {
                    Map<String, Object> project = managerProjects.stream()
                            .filter(p -> ((Number) p.get("id")).longValue() == entry.getProjectId())
                            .findFirst()
                            .orElse(null);
                    if (project == null || project.get("owner") == null) return null;
                    Map<String, Object> owner = (Map<String, Object>) project.get("owner");
                    return ((Number) owner.get("id")).longValue();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (requiredManagerIds.isEmpty()) {
            return TimeSheet.Status.SUBMITTED; // no managers, fallback
        }

        // 2️⃣ Fetch existing reviews for this timesheet
        List<TimeSheetReview> existingReviews = reviewRepo.findByTimeSheet_Id(ts.getId());
        Map<Long, TimeSheetReview.Status> reviewMap = existingReviews.stream()
                .collect(Collectors.toMap(TimeSheetReview::getManagerId, TimeSheetReview::getStatus));

        // 3️⃣ Compare and derive overall status
        boolean anyRejected = reviewMap.values().stream()
                .anyMatch(status -> status == TimeSheetReview.Status.REJECTED);

        boolean allReviewed = requiredManagerIds.stream().allMatch(reviewMap::containsKey);
        boolean allApproved = allReviewed && reviewMap.values().stream()
                .allMatch(status -> status == TimeSheetReview.Status.APPROVED);
        boolean anyApproved = reviewMap.values().stream()
                .anyMatch(status -> status == TimeSheetReview.Status.APPROVED);

        if (anyRejected) return TimeSheet.Status.REJECTED;
        else if (allApproved) return TimeSheet.Status.APPROVED;
        else if (anyApproved || !allReviewed) return TimeSheet.Status.PARTIALLY_APPROVED;
        else return TimeSheet.Status.SUBMITTED;
    }

    /** ✅ Improved to handle PARTIALLY_APPROVED at week level */
    @Transactional
    public void updateWeeklyTimeSheetReview(Long userId, Long weekInfoId) {
        List<TimeSheet> weekTimeSheets = timeSheetRepo.findByUserIdAndWeekInfo_Id(userId, weekInfoId);
        if (weekTimeSheets.isEmpty()) return;

        boolean anyRejected = weekTimeSheets.stream()
                .anyMatch(ts -> ts.getStatus() == TimeSheet.Status.REJECTED);
        boolean allApproved = weekTimeSheets.stream()
                .allMatch(ts -> ts.getStatus() == TimeSheet.Status.APPROVED);
    

        WeeklyTimeSheetReview.Status weeklyStatus;
        if (anyRejected) weeklyStatus = WeeklyTimeSheetReview.Status.REJECTED;
        else if (allApproved) weeklyStatus = WeeklyTimeSheetReview.Status.APPROVED;
        else weeklyStatus = WeeklyTimeSheetReview.Status.SUBMITTED;

        WeeklyTimeSheetReview weeklyReview = weeklyReviewRepo
                .findByUserIdAndWeekInfo_Id(userId, weekInfoId)
                .orElseGet(() -> {
                    WeeklyTimeSheetReview r = new WeeklyTimeSheetReview();
                    r.setUserId(userId);
                    r.setWeekInfo(weekInfoRepo.findById(weekInfoId)
                            .orElseThrow(() -> new IllegalArgumentException("Week not found")));
                    return r;
                });

        weeklyReview.setStatus(weeklyStatus);
        weeklyReview.setReviewedAt(LocalDateTime.now());
        weeklyReviewRepo.save(weeklyReview);
    }
}
