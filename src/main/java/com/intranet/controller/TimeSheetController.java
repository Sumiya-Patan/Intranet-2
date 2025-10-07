package com.intranet.controller;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import com.intranet.dto.TimeSheetDeleteRequest;
import com.intranet.dto.TimeSheetEntryCreateRequestDTO;
import com.intranet.dto.TimeSheetEntryDTO;
import com.intranet.dto.TimeSheetResponseDTO;
import com.intranet.dto.TimeSheetUpdateRequestDTO;
import com.intranet.dto.UserDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.security.CurrentUser;
import com.intranet.service.TimeSheetService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/timesheet")
public class TimeSheetController {

    @Autowired
    private TimeSheetService timeSheetService;

    @Autowired
    private TimeSheetRepo timeSheetRepository;

    @Value("${lms.api.base-url}")
    private String lmsBaseUrl;

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


   @Operation(summary = "Create a new timesheet")
    // @PreAuthorize("@endpointRoleService.hasAccess(#request.requestURI, #request.method, authentication)")
   @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
   @PostMapping("/create")
    public ResponseEntity<String> submitTimeSheet(
            @RequestParam(value = "workDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestBody List<TimeSheetEntryDTO> entries,
            @CurrentUser UserDTO user, HttpServletRequest request) {
        // If no workDate is passed, use today's date
        if (workDate == null) {
            workDate = LocalDate.now();
        }

        HttpEntity<Void> entity = buildEntityWithAuth();
         // 🔹 Step 1: Check if the date is a public holiday via LMS API
        String holidayUrl = String.format("%s/api/holidays/check?date=%s", lmsBaseUrl, workDate);
        try {
            ResponseEntity<Map<String, Object>> holidayResponse = restTemplate.exchange(
                    holidayUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> holidayBody = holidayResponse.getBody();
            if (holidayBody != null && "yes".equalsIgnoreCase((String) holidayBody.get("status"))) {
                return ResponseEntity.badRequest()
                        .body("Failed to submit timesheet on Public Holiday: "+holidayBody.get("message"));
            }
        } catch (Exception e) {
            // Optional: log the error but don't block timesheet submission if holiday API is down
            System.err.println("Holiday API check failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Unable to verify public holiday status. Please try again later.");
        }

        // 2. Check if timesheet already exists for this user and date
        boolean exists = timeSheetRepository.existsByUserIdAndWorkDate(user.getId(), workDate);
        if (exists) {
            return ResponseEntity.badRequest()
                    .body("Timesheet already submitted for " + workDate);
        }
        
        // Check for duplicates in the same request
        Map<String, Integer> entryCountMap = new HashMap<>();
        for (int i = 0; i < entries.size(); i++) {
        TimeSheetEntryDTO entry = entries.get(i);

        // String key = entry.getProjectId() + "-" + entry.getTaskId() + "-" 
        //             + entry.getFromTime() + "-" + entry.getToTime();

        String key = entry.getFromTime() + "-" + entry.getToTime();

        entryCountMap.put(key, entryCountMap.getOrDefault(key, 0) + 1);
        int count = entryCountMap.get(key);

        if (count > 1) {
            return ResponseEntity.badRequest()
                    .body("Duplicate entry found");
        }
        }

        BigDecimal totalHours = BigDecimal.ZERO;
        for (TimeSheetEntryDTO entry : entries) {
            if (entry.getFromTime() != null && entry.getToTime() != null) {
                long minutes = Duration.between(entry.getFromTime(), entry.getToTime()).toMinutes();
                long hoursPart = minutes / 60;
                long minutesPart = minutes % 60;

                // Convert to hours.minutes format, e.g., 5 hours 40 min => 5.40
                BigDecimal hoursWorked = new BigDecimal(hoursPart + "." + String.format("%02d", minutesPart));
                totalHours = totalHours.add(hoursWorked);

                // Store in the entry
                entry.setHoursWorked(hoursWorked);
            } else if (entry.getHoursWorked() != null) {
                totalHours = totalHours.add(entry.getHoursWorked());
            }
        }
        
        // Convert totalHours (HH.mm) to total minutes
        String[] parts = totalHours.toPlainString().split("\\.");
        long hours = Long.parseLong(parts[0]);
        long minutes = parts.length > 1 ? Long.parseLong(parts[1]) : 0;
        long totalMinutes = hours * 60 + minutes;

        if (totalMinutes < 8 * 60) { // 8 hours = 480 minutes
            return ResponseEntity.badRequest()
                    .body("Total hours must be at least 8 hours.");
        }

        try {
            timeSheetService.createTimeSheetWithEntries(user.getId(), workDate, entries);
            return ResponseEntity.ok("Timesheet submitted successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Timesheet submission failed");
        }
    }

    @Operation(summary = "Get user's timesheet history")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('GENERAL') or hasRole('HR')")
    // @PreAuthorize("@endpointRoleService.hasAccess(#request.requestURI, #request.method, authentication)")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
    @GetMapping("/history")
    public ResponseEntity<List<TimeSheetResponseDTO>> getTimeSheetHistory(@CurrentUser UserDTO user, HttpServletRequest request) {
            // @PathVariable Long userId , @CurrentUser UserDTO user) {
        List<TimeSheetResponseDTO> history = timeSheetService.getUserTimeSheetHistory(user.getId());
        return ResponseEntity.ok(history);
    }


    @Operation(summary = "Get user's timesheet history within a date range")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
    @GetMapping("/history/range")
    public ResponseEntity<?> getTimeSheetHistoryByDateRange(
        @CurrentUser UserDTO user,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        HttpServletRequest request) {

    if (startDate == null || endDate == null) {
        return ResponseEntity.badRequest().body("Start date and end date are required.");
    }

    // ✅ Validation: startDate must be strictly before endDate
    if (startDate.isAfter(endDate) || startDate.isEqual(endDate)) {
        return ResponseEntity.badRequest()
                .body("Invalid date range: startDate must be earlier than endDate.");
    }

    List<TimeSheetResponseDTO> history =
            timeSheetService.getUserTimeSheetHistoryByDateRange(user.getId(), startDate, endDate);

    return ResponseEntity.ok(history);
    }



    @Operation(summary = "Update a timesheet")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('HR')")
    // @PreAuthorize("@endpointRoleService.hasAccess(#request.requestURI, #request.method, authentication)")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
    @PutMapping("update/{timesheetId}")
    public ResponseEntity<String> partialUpdateTimesheet(
        @PathVariable Long timesheetId,
        @RequestBody TimeSheetUpdateRequestDTO updateRequest, HttpServletRequest request) {

    try {
        timeSheetService.partialUpdateTimesheet(timesheetId, updateRequest);
        return ResponseEntity.ok("Timesheet updated successfully.");
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
        return ResponseEntity.internalServerError().body("Unexpected error: " + e.getMessage());
    }
    }

    
    
    @Operation(summary = "Get timesheet by id")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('GENERAL') or hasRole('HR')")
    // @PreAuthorize("@endpointRoleService.hasAccess(#request.requestURI, #request.method, authentication)")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
    @GetMapping("/{id}")
    public TimeSheetResponseDTO getTimeSheetById(@PathVariable Long id, HttpServletRequest request) {
        return timeSheetService.getTimeSheetById(id);
    }


    @Operation(summary = "Add entries to a timesheet")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('GENERAL') or hasRole('HR')")
    // @PreAuthorize("@endpointRoleService.hasAccess(#request.requestURI, #request.method, authentication)")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
    @PutMapping("/add-entry/{timesheetId}")
    public ResponseEntity<String> addEntriesToTimeSheet(
            @PathVariable Long timesheetId,
            @RequestBody List<TimeSheetEntryCreateRequestDTO> newEntries, HttpServletRequest request) {

        return timeSheetService.addEntriesToTimeSheet(timesheetId, newEntries);
    }

    @Operation(summary = "Delete specific timesheet entries by timesheetId and entryIds")
    @DeleteMapping("/entries")
    @Transactional
    public ResponseEntity<String> deleteTimeSheetEntries(
        @RequestBody TimeSheetDeleteRequest request) {

    // 1. Validate input
    if (request.getTimesheetId() == null) {
        return ResponseEntity.badRequest().body("timesheetId is required.");
    }
    if (request.getEntryIds() == null || request.getEntryIds().isEmpty()) {
        return ResponseEntity.badRequest().body("At least one entryId must be provided.");
    }

    // 2. Fetch timesheet
    TimeSheet timeSheet = timeSheetRepository.findById(request.getTimesheetId())
            .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

    // 3. Prevent deletion if timesheet is Approved
    if ("Approved".equalsIgnoreCase(timeSheet.getStatus())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Cannot delete entries from an Approved timesheet.");
    }

    // 4. Filter only the entries that belong to this timesheet + match given entryIds
    List<TimeSheetEntry> entriesToDelete = timeSheet.getEntries().stream()
            .filter(entry -> request.getEntryIds().contains(entry.getTimesheetEntryId()))
            .toList();

    if (entriesToDelete.isEmpty()) {
        return ResponseEntity.badRequest()
                .body("No matching entries found in this timesheet for given IDs.");
    }

    // 5. Delete entries
    timeSheet.getEntries().removeAll(entriesToDelete);

    // 6. Check if any entries remain; if not, delete the timesheet itself
    if (timeSheet.getEntries().isEmpty()) {
        timeSheetRepository.delete(timeSheet);
        return ResponseEntity.ok("Deleted all entries and timesheet " + request.getTimesheetId());
    } else {
        timeSheetRepository.save(timeSheet);
        return ResponseEntity.ok("Deleted " + entriesToDelete.size() + " entry(s) from timesheet " + request.getTimesheetId());
    }
    }


}
