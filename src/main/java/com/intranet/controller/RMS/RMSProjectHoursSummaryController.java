package com.intranet.controller.RMS;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.rms.RMSProjectHoursSummaryResponseDTO;
import com.intranet.service.RMS.RMSProjectHoursSummaryService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class RMSProjectHoursSummaryController {

    private final RMSProjectHoursSummaryService service;

    @GetMapping("/RMS/project-hours-summary")
    @Operation(summary = "Get all projects with planned, actual, pending, utilization, billable, non-billable, and resource hours")
    public ResponseEntity<RMSProjectHoursSummaryResponseDTO> getProjectHoursSummary() {
        return ResponseEntity.ok(service.getProjectHoursSummary());
    }
}
