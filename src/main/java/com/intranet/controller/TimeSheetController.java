package com.intranet.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.TimeSheetEntryDTO;
import com.intranet.dto.TimeSheetResponseDTO;
import com.intranet.dto.TimeSheetUpdateRequestDTO;
import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.TimeSheetService;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/timesheet")
public class TimeSheetController {

    @Autowired
    private TimeSheetService timeSheetService;
    
   @PostMapping("/create")
    public ResponseEntity<String> submitTimeSheet(
            @RequestParam(value = "workDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestBody List<TimeSheetEntryDTO> entries,
            @CurrentUser UserDTO user) {
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

    @GetMapping("/history")
    public ResponseEntity<List<TimeSheetResponseDTO>> getTimeSheetHistory(@CurrentUser UserDTO user) {
            // @PathVariable Long userId , @CurrentUser UserDTO user) {
        List<TimeSheetResponseDTO> history = timeSheetService.getUserTimeSheetHistory(user.getId());
        return ResponseEntity.ok(history);
    }

    @PutMapping("update/{timesheetId}")
    public ResponseEntity<String> partialUpdateTimesheet(
        @PathVariable Long timesheetId,
        @RequestBody TimeSheetUpdateRequestDTO updateRequest) {

    try {
        timeSheetService.partialUpdateTimesheet(timesheetId, updateRequest);
        return ResponseEntity.ok("Timesheet updated successfully.");
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
        return ResponseEntity.internalServerError().body("Unexpected error: " + e.getMessage());
    }
    }
}
