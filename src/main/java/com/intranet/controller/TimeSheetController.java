package com.intranet.controller;


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

import com.intranet.dto.AddEntryDTO;
import com.intranet.dto.DeleteTimeSheetEntriesRequest;
import com.intranet.dto.TimeSheetEntryCreateDTO;
import com.intranet.dto.TimeSheetUpdateRequest;
import com.intranet.dto.WeekSummaryDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.WeekInfo;
import com.intranet.entity.WeeklyTimeSheetReview;
import com.intranet.repository.HolidayExcludeUsersRepo;
import com.intranet.repository.WeekInfoRepo;
import com.intranet.repository.WeeklyTimeSheetReviewRepo;
import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.TimeSheetService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.intranet.service.TimeUtil;


@RestController
@RequestMapping("/api/timesheet")
@CrossOrigin(origins = "*",allowedHeaders = "*")
public class TimeSheetController {

    @Autowired
    private TimeSheetService timeSheetService;

    @Autowired
    private HolidayExcludeUsersRepo holidayExcludeUsersRepo;

    @Autowired
    private WeekInfoRepo weekInfoRepo;

    @Autowired
    private WeeklyTimeSheetReviewRepo weeklyTimeSheetReviewRepo;


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

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${lms.api.base-url}")
    private String lmsBaseUrl;

    private int convertToMinutes(BigDecimal hhmm) {
    if (hhmm == null) return 0;
    String[] parts = hhmm.toPlainString().split("\\.");
    int hours = Integer.parseInt(parts[0]);
    int minutes = 0;
    if (parts.length > 1) {
        String minPart = parts[1];
        // Pad single digits (e.g. .5 -> .50)
        if (minPart.length() == 1) minPart += "0";
        minutes = Integer.parseInt(minPart);
        // Normalize incorrect formats like "6.67" → 6h + 67min = 7h 07min
        if (minutes >= 60) {
            hours += minutes / 60;
            minutes = minutes % 60;
        }
    }
    return hours * 60 + minutes;
    }


    @PostMapping("/create")
    @Operation(summary = "Submit a new timesheet")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') OR hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<?> submitTimeSheet(
        @CurrentUser UserDTO currentUser,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
        @RequestBody List<TimeSheetEntryCreateDTO> entries) {
    
    try {
        // 🔹 Step 1: Basic validations
        if (currentUser.getId() == null)
            return ResponseEntity.badRequest().body("User ID cannot be null");

        if (workDate == null)
            return ResponseEntity.badRequest().body("Work date cannot be null");

        if (entries == null || entries.isEmpty())
            return ResponseEntity.badRequest().body("TimeSheet entries cannot be empty");
        
        // 🔹 Step 1.1: Validate workDate belongs to current month
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        if (workDate.isBefore(firstDayOfMonth) || workDate.isAfter(lastDayOfMonth)) {
            return ResponseEntity.badRequest().body("Work date must belong to the current month.");
        }

        // 🔹 Step 1.2: Validate that the week is not approved
        Optional<WeekInfo> optionalWeekInfo = weekInfoRepo
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(workDate, workDate);

        if (optionalWeekInfo.isPresent()) {
            WeekInfo weekInfo = optionalWeekInfo.get();
            boolean isApproved = weeklyTimeSheetReviewRepo.existsByUserIdAndWeekInfoIdAndStatus(
                currentUser.getId(),
                weekInfo.getId(),
                WeeklyTimeSheetReview.Status.APPROVED
            );

            if (isApproved) {
                return ResponseEntity.badRequest()
                    .body("Cannot submit timesheet. Week " + weekInfo.getWeekNo() + " is already approved.");
            }
        }



        // 🔹 Step 2: Build auth headers
        HttpEntity<Void> entity = buildEntityWithAuth();

        // 🔹 Step 3: Check if the given date is a public holiday (via LMS API)
        String holidayUrl = String.format("%s/api/holidays/check?date=%s", lmsBaseUrl, workDate);
        boolean isPublicHoliday = false;
        String holidayMessage = null;

        try {
            ResponseEntity<Map<String, Object>> holidayResponse = restTemplate.exchange(
                    holidayUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> holidayBody = holidayResponse.getBody();
            if (holidayBody != null && "yes".equalsIgnoreCase((String) holidayBody.get("status"))) {
                isPublicHoliday = true;
                holidayMessage = (String) holidayBody.get("message");
            }

        } catch (Exception e) {
            System.err.println("⚠️ Holiday API check failed: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Unable to verify holiday status. Please try again later.");
        }

        // 🔹 Step 4: If it’s a holiday, verify if the user is allowed to submit
        if (isPublicHoliday) {
            boolean allowed = holidayExcludeUsersRepo
                    .existsByUserIdAndHolidayDate(currentUser.getId(), workDate);

            if (!allowed) {
                return ResponseEntity.badRequest()
                        .body("Cannot submit a timesheet because:" + holidayMessage);
            }
        }

        // 🔹 Step 5: Validate total worked hours
        BigDecimal totalWorked = TimeUtil.sumHours(
            entries.stream()
                .map(e -> TimeUtil.calculateHours(e.getFromTime(), e.getToTime()))
                .collect(Collectors.toList())
        );

        // Convert HH.MM -> total minutes for accurate comparison
        int totalMinutes = convertToMinutes(totalWorked);
        if (totalMinutes < 8 * 60) { // less than 8 hours
            return ResponseEntity.badRequest().body("Total working hours must be at least 8 hours.");
        }

        // 🔹 Step 6: Proceed with normal timesheet creation
        TimeSheet response = timeSheetService.createTimeSheet(currentUser.getId(), workDate, entries);
        if (response != null)
        return ResponseEntity.ok().body("Timesheet created successfully");
        
        else
        return ResponseEntity.badRequest().body("Failed to create timesheet");

    }
        
        catch(IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @PostMapping("/addEntries")
    @Operation(summary = "Add multiple entries to a timesheet")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') OR hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<String> addEntriesToTimeSheet(@RequestBody AddEntryDTO addEntryDTO) {
        String response = timeSheetService.addEntriesToTimeSheet(addEntryDTO);
        return ResponseEntity.ok().body(response);
    }
    

    @GetMapping("/weekly-summary-in-range")
    @Operation(summary = "Get timesheets grouped by week for a date range")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') OR hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<?> getWeeklyTimesheetSummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            List<WeekSummaryDTO> weeklySummary = timeSheetService.getTimesheetsByDateRange(startDate, endDate);
            return ResponseEntity.ok(weeklySummary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to retrieve weekly timesheet summary");
        }
    }

    @GetMapping("/debug/all")
    @Operation(summary = "Debug endpoint to check all timesheets in database")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') OR hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<?> debugAllTimesheets() {
        try {
            return ResponseEntity.ok(timeSheetService.debugGetAllTimesheets());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to retrieve timesheets: " + e.getMessage());
        }
    }
     
    
    @DeleteMapping("/deleteEntries/{timesheetId}")
    @Operation(summary = "Delete specific entries from a timesheet")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') OR hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<String> deleteEntries(@PathVariable Long timesheetId,@RequestBody DeleteTimeSheetEntriesRequest request) {
        String message = timeSheetService.deleteEntries(timesheetId,request);
        return ResponseEntity.ok(message);
    }
    
    
    @PutMapping("/updateEntries")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') OR hasAuthority('APPROVE_TIMESHEET')")
    @Operation(summary = "Update multiple entries in a timesheet")
    public ResponseEntity<String> updateEntries(@RequestBody TimeSheetUpdateRequest request) {
        String message = timeSheetService.updateEntries(request);
        return ResponseEntity.ok(message);
    }

}
