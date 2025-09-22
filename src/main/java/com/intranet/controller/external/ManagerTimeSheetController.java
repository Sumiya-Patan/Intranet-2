package com.intranet.controller.external;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.intranet.dto.TimeSheetEntryResponseDTO;
import com.intranet.dto.TimeSheetResponseDTO;
import com.intranet.dto.UserDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class ManagerTimeSheetController {

    private final TimeSheetRepo timeSheetRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${pms.api.base-url}")
    private String pmsBaseUrl;

    @Value("${ums.api.base-url}")
    private String umsBaseUrl;

    @Operation(summary = "Get timesheets of a manager")
    @GetMapping("/manager")
    @PreAuthorize("hasAuthority('APPROVE_TIMESHEET')")
    public ResponseEntity<List<TimeSheetResponseDTO>> getTimesheetsByManagerAndStatus(
            @CurrentUser UserDTO user,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {

        // Step 1: Prepare Authorization header from incoming request
        String authHeader = request.getHeader("Authorization");
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set("Authorization", authHeader); // forward the Bearer token
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Step 2: Get projects owned by this manager from external API
        String url = String.format("%s/projects/owner/%d", pmsBaseUrl, user.getId());
        ResponseEntity<List<Map<String, Object>>> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
        List<Map<String, Object>> projects = response.getBody();
        if (projects == null || projects.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Step 3: Collect all memberIds from the projects
        Set<Long> memberIds = projects.stream()
                .flatMap(p -> ((List<Map<String, Object>>) p.get("members")).stream())
                .map(m -> ((Number) m.get("id")).longValue())
                .collect(Collectors.toSet());

        // Step 4: Fetch all timesheets for these memberIds
        List<TimeSheet> allTimeSheets = timeSheetRepository.findByUserIdIn(memberIds);

        // Step 5: Optionally filter by status
        Stream<TimeSheet> filteredStream = allTimeSheets.stream();
        if (status != null && !status.isBlank()) {
            filteredStream = filteredStream.filter(ts -> status.equalsIgnoreCase(ts.getStatus()));
        }

        // Step 6: Build user cache by calling UMS
        Set<Long> userIds = allTimeSheets.stream().map(TimeSheet::getUserId).collect(Collectors.toSet());
        Map<Long, String> userCache = new HashMap<>();
        for (Long userId : userIds) {
            String userUrl = String.format("%s/admin/users/%d", umsBaseUrl, userId);
            try {
                ResponseEntity<Map<String, Object>> userResponse =
                        restTemplate.exchange(userUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
                Map<String, Object> userMap = userResponse.getBody();
                if (userMap != null) {
                    String firstName = (String) userMap.get("first_name");
                    String lastName = (String) userMap.get("last_name");
                    userCache.put(userId, firstName + " " + lastName);
                }
            } catch (Exception e) {
                userCache.put(userId, "User not from UMS"); // fallback if UMS fails
            }
        }

        // Step 7: Map to DTO
        List<TimeSheetResponseDTO> result = filteredStream.map(ts -> {
            List<TimeSheetEntryResponseDTO> entries = ts.getEntries().stream().map(entry -> {
                TimeSheetEntryResponseDTO dto = new TimeSheetEntryResponseDTO();
                dto.setTimesheetEntryId(entry.getTimesheetEntryId());
                dto.setProjectId(entry.getProjectId());
                dto.setTaskId(entry.getTaskId());
                dto.setDescription(entry.getDescription());
                dto.setWorkType(entry.getWorkType());
                dto.setHoursWorked(entry.getHoursWorked());
                dto.setFromTime(entry.getFromTime());
                dto.setToTime(entry.getToTime());
                dto.setOtherDescription(entry.getOtherDescription());
                return dto;
            }).toList();

            TimeSheetResponseDTO tsDto = new TimeSheetResponseDTO();
            tsDto.setTimesheetId(ts.getTimesheetId());
            tsDto.setUserId(ts.getUserId());
            tsDto.setUserName(userCache.get(ts.getUserId()));
            tsDto.setWorkDate(ts.getWorkDate());
            tsDto.setStatus(ts.getStatus());
            tsDto.setEntries(entries);
            return tsDto;
        }).toList();

        return ResponseEntity.ok(result);
    }
    @Operation(summary = "Debug permissions of the authenticated user")
    @GetMapping("/debug/permissions")
    public List<String> debugRoles(Authentication auth) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
    }
}
