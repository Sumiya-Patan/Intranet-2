package com.intranet.util.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.intranet.dto.lms.LeaveDTO;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveDirectoryService {

    @Value("${lms.api.base-url}")
    private String lmsBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetch all leaves for a given year & month.
     */
    @Cacheable(value = "leaveCache", key = "'leaves_'+#year+'_'+#month")
    public List<LeaveDTO> fetchLeaves(int year, int month, String authHeader) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = String.format("%s/api/leave-requests/getAllLeaves/%d/%d", lmsBaseUrl, year, month);

        try {

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("data")) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> leaves = (List<Map<String, Object>>) body.get("data");

            return leaves.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("⚠ Failed to fetch leaves: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Cacheable(value = "leaveCache", key = "'leaves__'+#year+'_'+#month")
    public List<LeaveDTO> fetchLeavesUserId(Long userId,int year, int month, String authHeader) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = String.format("%s/api/leave-requests/getLeaveRequests/%d/%d/%d", lmsBaseUrl, userId,year, month);

        try {

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("data")) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> leaves = (List<Map<String, Object>>) body.get("data");

            return leaves.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("⚠ Failed to fetch leaves: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private LeaveDTO convertToDto(Map<String, Object> row) {

        LeaveDTO dto = new LeaveDTO();

        dto.setEmployeeId(Long.valueOf(row.get("employeeId").toString()));
        dto.setStartDate(LocalDate.parse(row.get("startDate").toString()));
        dto.setEndDate(LocalDate.parse(row.get("endDate").toString()));
        dto.setReason((String) row.getOrDefault("reason", ""));
        dto.setStatus((String) row.getOrDefault("status", ""));

        return dto;
    }
}
