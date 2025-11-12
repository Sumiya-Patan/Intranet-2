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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

        public List<HolidayDTO> getUserHolidays(Long userId, int month) {
        HttpEntity<Void> entity = buildEntityWithAuth();

        // Step 1️⃣: Fetch holidays from LMS
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

        List<HolidayDTO> lmsHolidays = Optional.ofNullable(response.getBody()).orElse(Collections.emptyList());

        // Step 2️⃣: Get excluded holidays for this user
        List<HolidayExcludeUsers> excluded = repository.findByUserId(userId);
        Set<LocalDate> excludedDates = excluded.stream()
                .map(HolidayExcludeUsers::getHolidayDate)
                .collect(Collectors.toSet());

        // Step 3️⃣: Preload user cache (manager names)
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

        // Step 4️⃣: Build manager info map for excluded holidays
        Map<LocalDate, List<ManagerInfoDTO>> managerMap = excluded.stream()
                .collect(Collectors.groupingBy(
                        HolidayExcludeUsers::getHolidayDate,
                        Collectors.mapping(ex -> new ManagerInfoDTO(
                                ex.getManagerId(),
                                userCache.getOrDefault(ex.getManagerId(), "Unknown Manager")
                        ), Collectors.toList())
                ));

        // Step 5️⃣: Add weekends for the current month
        List<HolidayDTO> weekendHolidays = generateWeekendHolidays(month);

        // Step 6️⃣: Merge LMS holidays + weekend holidays
        List<HolidayDTO> allHolidays = new ArrayList<>();
        allHolidays.addAll(lmsHolidays);
        allHolidays.addAll(weekendHolidays);

        // Step 7️⃣: Build final response list with user exclusions
        return allHolidays.stream()
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
                .sorted(Comparator.comparing(HolidayDTO::getHolidayDate))
                .toList();
    }
        private List<HolidayDTO> generateWeekendHolidays(int month) {
        List<HolidayDTO> weekends = new ArrayList<>();
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();

        LocalDate firstDay = LocalDate.of(currentYear, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            DayOfWeek day = date.getDayOfWeek();

            // ✅ Saturday (6) or Sunday (7)
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                HolidayDTO dto = new HolidayDTO();
                dto.setHolidayId(0L); // default for generated holidays
                dto.setHolidayName(day == DayOfWeek.SATURDAY ? "Saturday" : "Sunday");
                dto.setHolidayDate(date);
                dto.setHolidayDescription("Casual Monthly Weekend");
                dto.setType("WEEKEND");
                dto.setCountry("India");
                dto.setState("All States");
                dto.setYear(currentYear);
                dto.setSubmitTimesheet(false);
                dto.setTimeSheetReviews("Weekend Holiday");
                dto.setAllowedManagers(List.of());
                weekends.add(dto);
            }
        }

        return weekends;
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

    public List<LocalDate> getUserHolidayDates(Long userId, int month) {
    HttpEntity<Void> entity = buildEntityWithAuth();

    // Step 1️⃣: Fetch holidays from LMS
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

    List<HolidayDTO> lmsHolidays = Optional.ofNullable(response.getBody()).orElse(Collections.emptyList());
    // Step 3️⃣: Generate weekend holidays
    List<HolidayDTO> weekendHolidays = generateWeekendHolidays(month);
    // Step 4️⃣: Merge LMS + weekend holidays
    List<HolidayDTO> allHolidays = new ArrayList<>();
    allHolidays.addAll(lmsHolidays);
    allHolidays.addAll(weekendHolidays);

    
    // Step 6️⃣: Extract only unique dates
    return allHolidays.stream()
            .map(HolidayDTO::getHolidayDate)
            .filter(Objects::nonNull)
            .distinct() // ensures unique dates
            .sorted()
            .collect(Collectors.toList());
    }

}

