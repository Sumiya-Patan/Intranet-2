package com.intranet.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class UserController {

    @Operation(summary = "Get Current user info")
    @PreAuthorize("@endpointRoleService.hasAccess(#request.requestURI, #request.method, authentication)")
    @GetMapping("/me")
    public UserDTO getUserInfo(@CurrentUser UserDTO user, HttpServletRequest request) {
        return user;
    }
    
}
