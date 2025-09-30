package com.intranet.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.StartEndDateReqDTO;
import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.TimeSheetService;

import io.swagger.v3.oas.annotations.Operation;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/dashboard")
public class TimeSheetSummaryController {


    @Autowired
    private TimeSheetService timeSheetService;

    @Operation(summary = "Get timesheet summary between two dates")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
    @PostMapping("/summary")
    public ResponseEntity<?> getTimeSheetSummary(
        @CurrentUser UserDTO user,
        @RequestBody StartEndDateReqDTO request) {

    LocalDate startDate = request.getStartDate();
    LocalDate endDate = request.getEndDate();

    if (startDate == null || endDate == null) {
        return ResponseEntity.badRequest().body("Start and End date are required");
    }

    if (startDate.isAfter(endDate)) {
        return ResponseEntity.badRequest().body("Start date cannot be after End date");
    }

    Map<String, Object> summary = timeSheetService.getSummary(user.getId(), startDate, endDate);

    return ResponseEntity.ok(summary);
    }

}
