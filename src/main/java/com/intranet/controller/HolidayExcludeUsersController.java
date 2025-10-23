package com.intranet.controller;

import com.intranet.dto.HolidayExcludeUsersRequestDTO;
import com.intranet.dto.UserDTO;

import com.intranet.security.CurrentUser;
import com.intranet.service.HolidayExcludeUsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/holiday-exclude-users")
public class HolidayExcludeUsersController {

    @Autowired
    private HolidayExcludeUsersService service;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('APPROVE_TIMESHEET')")
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
