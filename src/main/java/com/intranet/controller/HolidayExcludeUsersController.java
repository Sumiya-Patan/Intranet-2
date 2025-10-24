package com.intranet.controller;

import com.intranet.dto.HolidayExcludeUsersRequestDTO;
import com.intranet.dto.UserDTO;

import com.intranet.security.CurrentUser;
import com.intranet.service.HolidayExcludeUsersService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/holiday-exclude-users")
@CrossOrigin(origins = "*",allowedHeaders = "*")
public class HolidayExcludeUsersController {

    @Autowired
    private HolidayExcludeUsersService service;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('APPROVE_TIMESHEET')")
    @Operation(summary = "Create Holiday Exclude Users Entry by a manager")
    public ResponseEntity<String> createHolidayExclude(
        @CurrentUser UserDTO manager,
        @RequestBody HolidayExcludeUsersRequestDTO request) {
        try{
        String created = service.createHolidayExclude(manager.getId(),request);
        return ResponseEntity.ok(created);
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
}
