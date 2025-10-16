package com.intranet.controller;


import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.AddEntryDTO;
import com.intranet.dto.DeleteTimeSheetEntriesRequest;
import com.intranet.dto.TimeSheetEntryCreateDTO;
import com.intranet.dto.WeekSummaryDTO;
import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.TimeSheetService;

import io.swagger.v3.oas.annotations.Operation;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;


@RestController
@RequestMapping("/api/timesheet")
public class TimeSheetController {

    @Autowired
    private TimeSheetService timeSheetService;

    @PostMapping("/create")
    @Operation(summary = "Submit a new timesheet")
    public ResponseEntity<?> submitTimeSheet(
        @CurrentUser UserDTO currentUser,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
        @RequestBody List<TimeSheetEntryCreateDTO> entries) {
    
    try {
        if (currentUser.getId() == null) 
        return ResponseEntity.badRequest().body("User ID cannot be null");

        if (workDate == null) 
        return ResponseEntity.badRequest().body("Work date cannot be null");
       
        if (entries == null || entries.isEmpty())
        return ResponseEntity.badRequest().body("TimeSheet entries cannot be empty");

        String response = timeSheetService.createTimeSheet(currentUser.getId(), workDate, entries);
        return ResponseEntity.ok().body(response);
        }
        catch(Exception e) {
            return ResponseEntity.badRequest().body("Failed to create timesheet");
        }

    }

    @PostMapping("/addEntries")
    @Operation(summary = "Add multiple entries to a timesheet")
    public ResponseEntity<String> addEntriesToTimeSheet(@RequestBody AddEntryDTO addEntryDTO) {
        String response = timeSheetService.addEntriesToTimeSheet(addEntryDTO);
        return ResponseEntity.ok().body(response);
    }
    

    @GetMapping("/weekly-summary-in-range")
    @Operation(summary = "Get timesheets grouped by week for a date range")
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
    public ResponseEntity<?> debugAllTimesheets() {
        try {
            return ResponseEntity.ok(timeSheetService.debugGetAllTimesheets());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to retrieve timesheets: " + e.getMessage());
        }
    }
     @DeleteMapping("/entries")
     @Operation(summary = "Delete specific entries from a timesheet")
    public ResponseEntity<String> deleteEntries(@RequestBody DeleteTimeSheetEntriesRequest request) {
        String message = timeSheetService.deleteEntries(request);
        return ResponseEntity.ok(message);
    }
}
