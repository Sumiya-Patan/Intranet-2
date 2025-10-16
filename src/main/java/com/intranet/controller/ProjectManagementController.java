package com.intranet.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.UserDTO;
import com.intranet.dto.external.ProjectTaskView;
import com.intranet.security.CurrentUser;
import com.intranet.service.TimeSheetService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/project")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ProjectManagementController {

    @Autowired
    private TimeSheetService timesheetService;

     @Operation(summary = "Get Project and Task of a  current user")
    // @PreAuthorize("hasAuthority('EDIT_TIMESHEET') or hasAuthority('APPROVE_TIMESHEET')")
    @GetMapping
    public ResponseEntity<List<ProjectTaskView>> getTimesheetView(@CurrentUser UserDTO user, HttpServletRequest request) {
        List<ProjectTaskView> response = timesheetService.getUserTaskView(user.getId());
        return ResponseEntity.ok(response);
    }
    
}
