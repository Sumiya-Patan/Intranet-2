package com.intranet.service;

import com.intranet.dto.HolidayDTO;
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
        ResponseEntity<List<HolidayDTO>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});

        List<HolidayDTO> lmsHolidays = response.getBody();
        if (lmsHolidays == null || lmsHolidays.isEmpty()) {
            throw new IllegalArgumentException("Failed to fetch holidays from LMS.");
        }

        // 🔹 Step 2: Get excluded holidays for this user
        List<HolidayExcludeUsers> excluded = repository.findByUserId(userId);
        Set<LocalDate> excludedDates = excluded.stream()
                .map(HolidayExcludeUsers::getHolidayDate)
                .collect(Collectors.toSet());

        // 🔹 Step 3: Build manager info map for excluded holidays
    Map<LocalDate, List<ManagerInfoDTO>> managerMap = excluded.stream()
            .collect(Collectors.groupingBy(
                    HolidayExcludeUsers::getHolidayDate,
                    Collectors.mapping(ex -> new ManagerInfoDTO(
                            ex.getManagerId(),
                            getManagerName(ex.getManagerId(), entity) // fetch name
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
    private String getManagerName(Long managerId, HttpEntity<Void> entity) {

    try {
        
        String url = String.format("%s/admin/users/%d", umsBaseUrl, managerId);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});

        Map<String, Object> data = response.getBody();
        if (data != null) {
            String firstName = (String) data.get("first_name");
            String lastName = (String) data.get("last_name");
            return (firstName + " " + (lastName != null ? lastName : "")).trim();
        }
    } catch (Exception e) {
        // Fallback if not found or API error
    }
    return "Unknown Manager";
    }

    public List<HolidayExcludeUsers> getAllByManager(Long managerId) {
        return repository.findByManagerId(managerId);
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

