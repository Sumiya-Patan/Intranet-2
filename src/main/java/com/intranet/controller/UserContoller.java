package com.intranet.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;
import com.intranet.util.cache.UserDirectoryService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class UserContoller {

    
    private final UserDirectoryService userDirectoryService;

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('EDIT_TIMESHEET') OR hasAuthority('APPROVE_TIMESHEET')")
    @Operation(summary = "Get current user details")
    private UserDTO getCurrentUser(@CurrentUser UserDTO userDTO) {
        return userDTO;
        
    }


    @GetMapping("/users")
    @PreAuthorize("hasAuthority('TIMESHEET_ADMIN')")
    @Operation(summary = "Get all users from UMS")
    public ResponseEntity<?> getAllUsers(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || authHeader.isBlank()) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        try {

        List<Map<String,Object>> users = userDirectoryService.fetchAllUsers2(authHeader);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch users");
        }
    }
}
