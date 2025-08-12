package com.intranet.controller.external;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.intranet.dto.TimeSheetEntryResponseDTO;
import com.intranet.dto.TimeSheetResponseDTO;
import com.intranet.dto.UserDTO;
import com.intranet.dto.external.ManagerInfoDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.security.CurrentUser;
import com.intranet.service.external.ExternalProjectApiService;

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
    
    @GetMapping("/manager")
    public ResponseEntity<List<TimeSheetResponseDTO>> getTimesheetsByManagerAndStatus(
        @CurrentUser UserDTO user,
        @RequestParam(required = false) String status
    ) {

    // Step 1: Get projects owned by this manager from external API
    String url = String.format("%s/projects/owner/%d", pmsBaseUrl, user.getId());
    ResponseEntity<List<Map<String, Object>>> response =
            restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

    List<Map<String, Object>> projects = response.getBody();
    if (projects == null || projects.isEmpty()) {
        return ResponseEntity.ok(Collections.emptyList());
    }

    // Step 2: Collect all memberIds from the projects
    Set<Long> memberIds = projects.stream()
            .flatMap(p -> ((List<Map<String, Object>>) p.get("members")).stream())
            .map(m -> ((Number) m.get("id")).longValue())
            .collect(Collectors.toSet());

    // Step 3: Fetch all timesheets for these memberIds
    List<TimeSheet> allTimeSheets = timeSheetRepository.findByUserIdIn(memberIds);

    // Step 4: Optionally filter by status
    Stream<TimeSheet> filteredStream = allTimeSheets.stream();
    if (status != null && !status.isBlank()) {
        filteredStream = filteredStream.filter(ts -> status.equalsIgnoreCase(ts.getStatus()));
    }

    // Step 5: Map to DTO
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
        tsDto.setWorkDate(ts.getWorkDate());
        tsDto.setStatus(ts.getStatus());
        tsDto.setEntries(entries);
        return tsDto;
    }).toList();

    return ResponseEntity.ok(result);
}
}
