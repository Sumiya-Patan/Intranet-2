package com.intranet.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.TimeSheetEntryCreateRequestDTO;
import com.intranet.dto.TimeSheetEntryDTO;
import com.intranet.dto.TimeSheetResponseDTO;
import com.intranet.dto.TimeSheetUpdateRequestDTO;
import com.intranet.dto.UserDTO;
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
        try {
            timeSheetService.createTimeSheetWithEntries(user.getId(), workDate, entries);
            return ResponseEntity.ok("Timesheet submitted successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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

        boolean success = timeSheetService.addEntriesToTimeSheet(timesheetId, newEntries);
        if (success) {
            return ResponseEntity.ok("New entry(ies) added to timesheet: " + timesheetId);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
