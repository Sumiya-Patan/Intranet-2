package com.intranet.controller;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.intranet.dto.ActionStatusDTO;
import com.intranet.dto.BulkTimeSheetReviewRequest;
import com.intranet.dto.TimeSheetResponseDTO;
import com.intranet.dto.UserDTO;
import com.intranet.dto.external.TimeSheetReviewRequest;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.entity.TimeSheetReview;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.TimeSheetReviewRepo;
import com.intranet.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;



@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class TimeSheetReviewController {

    @Autowired
    private final TimeSheetRepo timeSheetRepository;

    @Autowired
    private final TimeSheetReviewRepo reviewRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${pms.api.base-url}")
    private String pmsBaseUrl;

    @Value("${ums.api.base-url}")
    private String umsBaseUrl;

    @Operation(summary = "Review a timesheet by manager")
    @PreAuthorize("hasAuthority('APPROVE_TIMESHEET')")
    @PutMapping("/review")
    @Transactional
    public ResponseEntity<TimeSheetResponseDTO> reviewTimesheet(
        @CurrentUser UserDTO user,
        @RequestParam String status,
        @RequestBody TimeSheetReviewRequest request1,
        HttpServletRequest request) {

    // 1. Validate input
    if (!"Approved".equalsIgnoreCase(status) && !"Rejected".equalsIgnoreCase(status)) {
        return ResponseEntity.badRequest().build();
    }
    if ("Rejected".equalsIgnoreCase(status) &&
        (request1.getComment() == null || request1.getComment().isBlank())) {
        return ResponseEntity.badRequest().build();
    }

    // 2. Fetch timesheet
    TimeSheet timeSheet = timeSheetRepository.findById(request1.getTimesheetId())
            .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

    // 3. Setup authorization header for PMS calls
    String authHeader = request.getHeader("Authorization");
    HttpHeaders headers = new HttpHeaders();
    if (authHeader != null && !authHeader.isBlank()) {
        headers.set("Authorization", authHeader);
    }
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    // 4. Verify manager owns at least one project in the timesheet
    String ownerProjectsUrl = String.format("%s/projects/owner/%d", pmsBaseUrl, user.getId());
    ResponseEntity<List<Map<String, Object>>> ownerProjectsResp =
            restTemplate.exchange(ownerProjectsUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
    List<Map<String, Object>> ownerProjects = ownerProjectsResp.getBody();

    Set<Long> managerProjectIds = (ownerProjects == null) ? Collections.emptySet() :
            ownerProjects.stream()
                    .map(p -> ((Number) p.get("id")).longValue())
                    .collect(Collectors.toSet());

    Set<Long> timesheetProjectIds = timeSheet.getEntries().stream()
            .map(TimeSheetEntry::getProjectId)
            .collect(Collectors.toSet());

    boolean allowed = timesheetProjectIds.stream().anyMatch(managerProjectIds::contains);
    if (!allowed) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // 5. Create or update review
    TimeSheetReview existingReview = reviewRepository
            .findByTimeSheetAndManagerId(timeSheet, user.getId())
            .orElse(null);

    if (existingReview != null) {
        existingReview.setAction(status);
        existingReview.setComment(request1.getComment());
        existingReview.setReviewedAt(LocalDateTime.now());
        reviewRepository.save(existingReview);
    } else {
        TimeSheetReview review = new TimeSheetReview();
        review.setTimeSheet(timeSheet);
        review.setManagerId(user.getId());
        review.setAction(status);
        review.setComment(request1.getComment());
        review.setReviewedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }

    // 6. Recompute overall status based on this timesheet’s projects
    String projectsUrl = String.format("%s/projects", pmsBaseUrl);
    ResponseEntity<List<Map<String, Object>>> allProjectsResp =
            restTemplate.exchange(projectsUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
    List<Map<String, Object>> allProjects = allProjectsResp.getBody();

    // Extract owners only for this timesheet’s projects
    Set<Long> approverIds = new HashSet<>();
    Map<Long, String> approverNames = new HashMap<>();
    if (allProjects != null) {
        for (Map<String, Object> p : allProjects) {
            Long pid = ((Number) p.get("id")).longValue();
            if (!timesheetProjectIds.contains(pid)) continue;

            Map<String, Object> owner = (Map<String, Object>) p.get("owner");
            if (owner != null) {
                Long ownerId = ((Number) owner.get("id")).longValue();
                String ownerName = (String) owner.get("name");
                approverIds.add(ownerId);
                approverNames.put(ownerId, ownerName);
            }
        }
    }

    // Build actionStatus list
    List<ActionStatusDTO> actionStatus = new ArrayList<>();
    for (Long approverId : approverIds) {
        Optional<TimeSheetReview> revOpt = reviewRepository.findByTimeSheetAndManagerId(timeSheet, approverId);
        String action = revOpt.map(TimeSheetReview::getAction).orElse("Pending");
        String approverName = approverNames.getOrDefault(approverId, "Unknown Manager");
        actionStatus.add(new ActionStatusDTO(approverId, approverName, action));
    }

    // 7. Apply status rules
    boolean anyRejected = actionStatus.stream()
            .anyMatch(a -> "Rejected".equalsIgnoreCase(a.getStatus()));

    boolean allApproved = !actionStatus.isEmpty() && actionStatus.stream()
            .allMatch(a -> "Approved".equalsIgnoreCase(a.getStatus()));

    boolean allPending = !actionStatus.isEmpty() && actionStatus.stream()
            .allMatch(a -> "Pending".equalsIgnoreCase(a.getStatus()));

    // boolean partiallyApproved = !allApproved && !allPending && !anyRejected 
    //         && actionStatus.stream().anyMatch(a -> "Approved".equalsIgnoreCase(a.getStatus()));

    String overall;
    if (anyRejected) {
        overall = "Rejected";
    } else if (allApproved) {
        overall = "Approved";
    } else if (allPending) {
        overall = "Pending";
    } else {
        overall = "Pending"; // fallback
    }


    // 8. Save updated timesheet
    timeSheet.setStatus(overall);
    timeSheet.setUpdatedAt(LocalDateTime.now());
    timeSheetRepository.save(timeSheet);

    // 9. Build response DTO
    TimeSheetResponseDTO responseDto = new TimeSheetResponseDTO();
    responseDto.setTimesheetId(timeSheet.getTimesheetId());
    responseDto.setUserId(timeSheet.getUserId());
    responseDto.setWorkDate(timeSheet.getWorkDate());
    responseDto.setStatus(overall);
    responseDto.setActionStatus(actionStatus);

    return ResponseEntity.ok(responseDto);
    }
    
    @Operation(summary = "Bulk review timesheets by manager")
    // @PreAuthorize("@endpointRoleService.hasAccess(#request.requestURI, #request.method, authentication)")
    @PreAuthorize("hasAuthority('APPROVE_TIMESHEET')")
    @PutMapping("/review/bulk")
    public ResponseEntity<String> bulkReviewTimesheets(
            @CurrentUser UserDTO user,
            @RequestBody BulkTimeSheetReviewRequest bulkRequest,
            HttpServletRequest request
        ) {
        String status = bulkRequest.getStatus();

        if (!"Approved".equalsIgnoreCase(status) && !"Rejected".equalsIgnoreCase(status)) {
            return ResponseEntity.badRequest().body("Invalid status. Must be APPROVED or REJECTED.");
        }

        if ("Rejected".equalsIgnoreCase(status) && (bulkRequest.getComment() == null || bulkRequest.getComment().isBlank())) {
            return ResponseEntity.badRequest().body("Comment is required for rejected timesheets.");
        }

        // Fetch all timesheets by IDs
        var timeSheets = timeSheetRepository.findAllById(bulkRequest.getTimesheetIds());

        if (timeSheets.isEmpty()) {
            return ResponseEntity.badRequest().body("No valid timesheets found for given IDs.");
        }

        // 🔍 Check if all are still Pending
    boolean hasNonPending = timeSheets.stream()
            .anyMatch(ts -> !"Pending".equalsIgnoreCase(ts.getStatus()));

    if (hasNonPending) {
        return ResponseEntity.badRequest().body("Some timesheets are already updated.");
    }

        for (TimeSheet ts : timeSheets) {
            ts.setStatus(status);
            ts.setUpdatedAt(LocalDateTime.now());
            timeSheetRepository.save(ts);

            // Check if review already exists for this timesheet + manager
            TimeSheetReview existingReview = reviewRepository
                    .findByTimeSheetAndManagerId(ts, user.getId())
                    .orElse(null);

            if (existingReview != null) {
                // Update existing review
                existingReview.setAction(status);
                existingReview.setComment(bulkRequest.getComment());
                existingReview.setReviewedAt(LocalDateTime.now());
                reviewRepository.save(existingReview);
            } else {
                // Create new review
                TimeSheetReview review = new TimeSheetReview();
                review.setTimeSheet(ts);
                review.setManagerId(user.getId());
                review.setAction(status);
                review.setComment(bulkRequest.getComment());
                review.setReviewedAt(LocalDateTime.now());
                reviewRepository.save(review);
            }
        }

        return ResponseEntity.ok("Bulk timesheet review completed successfully.");
        }

}
