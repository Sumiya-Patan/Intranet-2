package com.intranet.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserContoller {

    @GetMapping("/me")
    @Operation(summary = "Get current user details")
    private UserDTO getCurrentUser(@CurrentUser UserDTO userDTO) {
        return userDTO;
        
    }
}
