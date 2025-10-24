package com.intranet.controller.external;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.intranet.dto.UserDTO;
import com.intranet.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/manager")
@CrossOrigin(origins = "*",allowedHeaders = "*")
public class ManagerTimeSheetController {

    @Value("${pms.api.base-url}")
    private String pmsBaseUrl;

    @Value("${ums.api.base-url}")
    private String umsBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();


    @Operation(summary = "Get all users under a manager")
    @PreAuthorize("hasAuthority('APPROVE_TIMESHEET')")
    @GetMapping("/users")
    // @PreAuthorize("hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<List<Map<String, Object>>> getUsersUnderManager(
        @CurrentUser UserDTO user,
        HttpServletRequest request) {

    // Step 1: Prepare Authorization header
    String authHeader = request.getHeader("Authorization");
    HttpHeaders headers = new HttpHeaders();
    if (authHeader != null && !authHeader.isBlank()) {
        headers.set("Authorization", authHeader); // forward the Bearer token
    }
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    // Step 2: Get projects owned by this manager from PMS API
    String url = String.format("%s/projects/owner", pmsBaseUrl);
    ResponseEntity<List<Map<String, Object>>> response =
            restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
    List<Map<String, Object>> projects = response.getBody();

    if (projects == null || projects.isEmpty()) {
        return ResponseEntity.ok(Collections.emptyList());
    }

    // Step 3: Collect all unique member IDs from all projects
    Set<Long> memberIds = projects.stream()
            .flatMap(p -> Optional.ofNullable((List<Map<String, Object>>) p.get("members"))
                    .orElse(Collections.emptyList()).stream())
            .map(m -> ((Number) m.get("id")).longValue())
            .collect(Collectors.toSet());

    if (memberIds.isEmpty()) {
        return ResponseEntity.ok(Collections.emptyList());
    }

    // Step 4: Build user details by calling UMS API
    List<Map<String, Object>> users = new ArrayList<>();
    for (Long userId : memberIds) {
        String userUrl = String.format("%s/admin/users/%d", umsBaseUrl, userId);
        try {
            ResponseEntity<Map<String, Object>> userResponse =
                    restTemplate.exchange(userUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
            Map<String, Object> userMap = userResponse.getBody();
            if (userMap != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", userId);
                userInfo.put("firstName", userMap.getOrDefault("first_name", ""));
                userInfo.put("lastName", userMap.getOrDefault("last_name", ""));
                userInfo.put("email", userMap.getOrDefault("mail", ""));
                userInfo.put("fullName", userMap.getOrDefault("first_name", "") + " " + userMap.getOrDefault("last_name", ""));
                users.add(userInfo);
            }
        } catch (Exception e) {
            // fallback if user not found in UMS
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("id", userId);
            fallback.put("fullName", "User not found in UMS");
            users.add(fallback);
        }
    }

    return ResponseEntity.ok(users);
    }
    
}
