package com.intranet.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.TimeSheetEntryCreateDTO;
import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.TimeSheetService;

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
    public ResponseEntity<?> submitTimeSheet(
        @CurrentUser UserDTO currentUser,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
        @RequestBody List<TimeSheetEntryCreateDTO> entries) {
    
    try {
    String saved = timeSheetService.createTimeSheet(currentUser.getId(), workDate, entries);
    return ResponseEntity.ok(saved);}
    catch(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body("Failed to create timesheet");
    }

    }
}
