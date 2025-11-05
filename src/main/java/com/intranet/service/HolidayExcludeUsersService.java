package com.intranet.service;

import com.intranet.dto.HolidayDTO;
import com.intranet.dto.HolidayExcludeResponseDTO;
import com.intranet.dto.HolidayExcludeUsersRequestDTO;
import com.intranet.dto.external.ManagerInfoDTO;
import com.intranet.entity.HolidayExcludeUsers;
import com.intranet.repository.HolidayExcludeUsersRepo;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HolidayExcludeUsersService {

    private HttpEntity<Void> buildEntityWithAuth() {

    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null) {
        return (HttpEntity<Void>) HttpEntity.EMPTY;
    }

    HttpServletRequest request = attrs.getRequest();
    String authHeader = request.getHeader("Authorization");

    HttpHeaders headers = new HttpHeaders();
    if (authHeader != null && !authHeader.isBlank()) {
        headers.set("Authorization", authHeader);
    }

    return new HttpEntity<>(headers);
    }

    @Autowired
    private HolidayExcludeUsersRepo repository;

    @Value("${lms.api.base-url}")
    private String lmsBaseUrl;

    @Value("${ums.api.base-url}")
    private String umsBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String createHolidayExclude(Long managerId,HolidayExcludeUsersRequestDTO request) {

        // Optionally: you can check if record already exists with same user, manager, and date
        if (repository.existsByUserIdAndManagerIdAndHolidayDate(request.getUserId(), managerId, request.getHolidayDate())) {
            throw new IllegalArgumentException("Holiday exclusion already exists for this user on the specified date");
        }
        HolidayExcludeUsers entity = new HolidayExcludeUsers();
        entity.setUserId(request.getUserId());
        entity.setManagerId(managerId);
        entity.setHolidayDate(request.getHolidayDate());
        entity.setReason(request.getReason());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
        return "Holiday exclusion created successfully";
    }

    public List<HolidayDTO> getUserHolidays(Long userId,int month) {
  
        HttpEntity<Void> entity = buildEntityWithAuth();

        // Step 1: Fetch holidays from LMS
        String url = String.format("%s/api/holidays/month/%d", lmsBaseUrl, month);

        ResponseEntity<List<HolidayDTO>> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<HolidayDTO>>() {}
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch holidays from LMS for month: " + month, e);
        }

        // ✅ Handle empty or missing holiday list
        List<HolidayDTO> lmsHolidays = response.getBody();

        // ✅ If holidays list is empty, allow method to continue normally
        if (lmsHolidays.isEmpty()) {
            return Collections.emptyList(); // or skip return, depending on your flow
        }

        // 🔹 Step 2: Get excluded holidays for this user
        List<HolidayExcludeUsers> excluded = repository.findByUserId(userId);
        Set<LocalDate> excludedDates = excluded.stream()
                .map(HolidayExcludeUsers::getHolidayDate)
                .collect(Collectors.toSet());

            // 🔹 Step 2: Preload all users (including managers) from UMS to avoid repeated calls
        Map<Long, String> userCache = new HashMap<>();
        try {
            String umsUrl = String.format("%s/admin/users?page=1&limit=500", umsBaseUrl);
            ResponseEntity<Map<String, Object>> umsResponse = restTemplate.exchange(
                    umsUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = umsResponse.getBody();
            if (body != null && body.containsKey("users")) {
                Object usersObj = body.get("users");
                if (usersObj instanceof List<?>) {
                    for (Object obj : (List<?>) usersObj) {
                        if (obj instanceof Map) {
                            Map<?, ?> u = (Map<?, ?>) obj;
                            Number idNum = (Number) u.get("user_id");
                            if (idNum != null) {
                                Long id = idNum.longValue();
                                String firstName = u.get("first_name") != null ? u.get("first_name").toString().trim() : "";
                                String lastName  = u.get("last_name")  != null ? u.get("last_name").toString().trim()  : "";
                                String fullName  = (firstName + " " + lastName).trim();
                                userCache.put(id, fullName.isEmpty() ? "Unknown User" : fullName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to load users from UMS: " + e.getMessage());
        }

        // 🔹 Step 3: Build manager info map using cached names
        Map<LocalDate, List<ManagerInfoDTO>> managerMap = excluded.stream()
                .collect(Collectors.groupingBy(
                        HolidayExcludeUsers::getHolidayDate,
                        Collectors.mapping(ex -> new ManagerInfoDTO(
                                ex.getManagerId(),
                                userCache.getOrDefault(ex.getManagerId(), "Unknown Manager")
                        ), Collectors.toList())
                ));


    // 🔹 Step 4: Build final response
    return lmsHolidays.stream()
            .map(holiday -> {
                boolean isExcluded = excludedDates.contains(holiday.getHolidayDate());
                holiday.setSubmitTimesheet(isExcluded);

                if (isExcluded) {
                    holiday.setTimeSheetReviews("Allowed to Submit on Holiday");
                    holiday.setAllowedManagers(managerMap.getOrDefault(
                            holiday.getHolidayDate(), List.of()
                    ));
                } else {
                    holiday.setTimeSheetReviews("General Holiday");
                    holiday.setAllowedManagers(List.of());
                }

                return holiday;
            })
            .toList();
    }

    public List<HolidayExcludeResponseDTO> getAllByManager(Long managerId) {
    List<HolidayExcludeUsers> users = repository.findByManagerId(managerId);
    if (users.isEmpty()) {
        return Collections.emptyList();
    }

    HttpEntity<Void> entity = buildEntityWithAuth();

    /// --- Cache to avoid multiple UMS calls for the same user ---
    Map<Long, String> userCache = new HashMap<>();

    // 🧠 Pre-fetch all required users from UMS in one go
    try {
        String umsUrl = String.format("%s/admin/users?page=1&limit=500", umsBaseUrl);
        ResponseEntity<Map<String, Object>> umsResponse = restTemplate.exchange(
                umsUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> body = umsResponse.getBody();
        if (body != null && body.containsKey("users")) {
            Object usersObj = body.get("users");
            if (usersObj instanceof List<?>) {
                for (Object obj : (List<?>) usersObj) {
                    if (obj instanceof Map) {
                        Map<?, ?> u = (Map<?, ?>) obj;
                        Number idNum = (Number) u.get("user_id");
                        if (idNum != null) {
                            Long id = idNum.longValue();
                            String firstName = u.get("first_name") != null ? u.get("first_name").toString().trim() : "";
                            String lastName  = u.get("last_name")  != null ? u.get("last_name").toString().trim()  : "";
                            String fullName  = (firstName + " " + lastName).trim();
                            userCache.put(id, fullName.isEmpty() ? "Unknown User" : fullName);
                        }
                    }
                }
            }
        }
    } catch (Exception e) {
        System.err.println("⚠️ Failed to prefetch users from UMS: " + e.getMessage());
    }

    // --- Now build the DTO list ---
    return users.stream().map(user -> {
        HolidayExcludeResponseDTO dto = new HolidayExcludeResponseDTO();
        dto.setId(user.getId());
        dto.setUserId(user.getUserId());
        dto.setManagerId(user.getManagerId());
        dto.setHolidayDate(user.getHolidayDate());
        dto.setReason(user.getReason());

        // ✅ Get username from cache (no UMS call here)
        String userName = userCache.getOrDefault(user.getUserId(), "Unknown User");
        dto.setUserName(userName);

        return dto;
        }).collect(Collectors.toList());
        }



    public String deleteHolidayExclude(Long managerId, Long id) {
        HolidayExcludeUsers existing = repository.findByIdAndManagerId(id, managerId);
        if (existing == null)
            throw new IllegalArgumentException("Record not found or not authorized");

        repository.delete(existing);
        return "Holiday Exclude entry deleted successfully.";
    }

        public String updateHolidayExclude(Long managerId, Long id, HolidayExcludeUsersRequestDTO dto) {
        HolidayExcludeUsers existing = repository.findByIdAndManagerId(id, managerId);
        if (existing == null)
            throw new IllegalArgumentException("Record not found or not authorized");

        existing.setReason(dto.getReason());
        existing.setHolidayDate(dto.getHolidayDate());
        existing.setUpdatedAt(LocalDateTime.now());

        repository.save(existing);
        return "Holiday Exclude entry updated successfully.";
    }


}

