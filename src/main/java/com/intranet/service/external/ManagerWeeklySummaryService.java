package com.intranet.service.external;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.intranet.dto.TimeSheetEntrySummaryDTO;
import com.intranet.dto.TimeSheetSummaryDTO;
import com.intranet.dto.WeekSummaryDTO;
import com.intranet.dto.external.ManagerWeeklySummaryDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.WeekInfo;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.service.TimeUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManagerWeeklySummaryService {

    private final RestTemplate restTemplate;
    private final TimeSheetRepo timeSheetRepository;

    @Value("${pms.api.base-url}")
    private String pmsBaseUrl;

    @Value("${ums.api.base-url}")
    private String umsBaseUrl;

    public List<ManagerWeeklySummaryDTO> getWeeklySubmittedTimesheetsByManager(Long managerId, String authHeader) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", authHeader);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    // Step 1: Get all projects owned by this manager
    String url = String.format("%s/projects/owner", pmsBaseUrl);
    ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
    List<Map<String, Object>> projects = response.getBody();
    if (projects == null || projects.isEmpty()) return Collections.emptyList();

    // Step 2: Get all member IDs under these projects
    Set<Long> memberIds = projects.stream()
            .flatMap(p -> ((List<Map<String, Object>>) p.get("members")).stream())
            .map(m -> ((Number) m.get("id")).longValue())
            .collect(Collectors.toSet());
    if (memberIds.isEmpty()) return Collections.emptyList();

    // Step 3: Fetch all non-draft timesheets of these members
    List<TimeSheet> allSheets = timeSheetRepository.findNonDraftByUserIds(memberIds);
    if (allSheets.isEmpty()) return Collections.emptyList();

    // Step 4: Filter timesheets where manager has at least one project entry
    List<TimeSheet> managerSheets = allSheets.stream()
            .filter(ts -> ts.getEntries().stream()
                    .anyMatch(e -> projects.stream()
                            .anyMatch(p -> ((Number)p.get("id")).longValue() == e.getProjectId())))
            .toList();

    if (managerSheets.isEmpty()) return Collections.emptyList();

    // Step 5: Fetch usernames from UMS
    Map<Long, String> userCache = new HashMap<>();
    for (Long uid : memberIds) {
        String userUrl = String.format("%s/admin/users/%d", umsBaseUrl, uid);
        try {
            ResponseEntity<Map<String, Object>> userResponse =
                    restTemplate.exchange(userUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
            Map<String, Object> userMap = userResponse.getBody();
            if (userMap != null) {
                String firstName = (String) userMap.get("first_name");
                String lastName = (String) userMap.get("last_name");
                userCache.put(uid, (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : ""));
            }
        } catch (Exception e) {
            userCache.put(uid, "Unknown User");
        }
    }

    // Step 6: Group by userId and build weekly summary
    return managerSheets.stream()
            .collect(Collectors.groupingBy(TimeSheet::getUserId))
            .entrySet().stream()
            .map(entry -> {
                Long userId = entry.getKey();
                List<TimeSheet> userSheets = entry.getValue();

                List<WeekSummaryDTO> weekSummaries = userSheets.stream()
                        .collect(Collectors.groupingBy(ts -> ts.getWeekInfo().getWeekNo()))
                        .entrySet().stream()
                        .map(weekEntry -> {
                            Integer weekNo = weekEntry.getKey();
                            List<TimeSheet> weekSheets = weekEntry.getValue();
                            WeekInfo week = weekSheets.get(0).getWeekInfo();

                            // Map to DTO with manager-specific status
                            List<TimeSheetSummaryDTO> timeSheetDTOs = weekSheets.stream()
                                    .map(ts -> mapToSummaryDTOForManager(ts, managerId, projects))
                                    .toList();

                            BigDecimal totalHours = TimeUtil.sumHours(
                                    timeSheetDTOs.stream().map(TimeSheetSummaryDTO::getHoursWorked).toList()
                            );

                            WeekSummaryDTO weekDTO = new WeekSummaryDTO();
                            weekDTO.setWeekId(weekNo.longValue());
                            weekDTO.setStartDate(week.getStartDate());
                            weekDTO.setEndDate(week.getEndDate());
                            weekDTO.setTotalHours(totalHours);
                            weekDTO.setTimesheets(timeSheetDTOs);
                            return weekDTO;
                        }).toList();

                ManagerWeeklySummaryDTO managerDTO = new ManagerWeeklySummaryDTO();
                managerDTO.setUserId(userId);
                managerDTO.setUserName(userCache.getOrDefault(userId, "Unknown"));
                managerDTO.setWeeklySummary(weekSummaries);
                return managerDTO;
            }).toList();
}

/**
 * Map timesheet to DTO for a specific manager.
 * Only timesheets where the manager has at least one entry will be passed here.
 * All entries are shown.
 * Status is manager-specific.
 */
private TimeSheetSummaryDTO mapToSummaryDTOForManager(TimeSheet ts, Long managerId, List<Map<String, Object>> managerProjects) {
    List<TimeSheetEntrySummaryDTO> entries = ts.getEntries().stream().map(e -> {
        TimeSheetEntrySummaryDTO dto = new TimeSheetEntrySummaryDTO();
        dto.setTimesheetEntryid(e.getId());
        dto.setProjectId(e.getProjectId());
        dto.setTaskId(e.getTaskId());
        dto.setDescription(e.getDescription());
        dto.setWorkLocation(e.getWorkLocation());
        dto.setFromTime(e.getFromTime() != null ? e.getFromTime().toLocalTime().toString() : null);
        dto.setToTime(e.getToTime() != null ? e.getToTime().toLocalTime().toString() : null);
        dto.setHoursWorked(e.getHoursWorked());
        dto.setOtherDescription(e.getOtherDescription());
        return dto;
    }).toList();

    TimeSheetSummaryDTO dto = new TimeSheetSummaryDTO();
    dto.setTimesheetId(ts.getId());
    dto.setWorkDate(ts.getWorkDate());
    dto.setHoursWorked(ts.getHoursWorked());
    dto.setEntries(entries);

    // Manager-specific status
    String managerStatus = ts.getReviews().stream()
            .filter(r -> r.getManagerId().equals(managerId))
            .map(r -> r.getStatus().name())
            .findFirst()
            .orElse("SUBMITTED");
    dto.setStatus(managerStatus);

    return dto;
}

}
