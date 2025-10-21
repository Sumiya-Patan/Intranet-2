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

        // 🔹 Step 1: Get all projects owned by manager
        String url = String.format("%s/projects/owner", pmsBaseUrl);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
        List<Map<String, Object>> projects = response.getBody();
        if (projects == null || projects.isEmpty()) return Collections.emptyList();

        // 🔹 Step 2: Get all members under these projects
        Set<Long> memberIds = projects.stream()
                .flatMap(p -> ((List<Map<String, Object>>) p.get("members")).stream())
                .map(m -> ((Number) m.get("id")).longValue())
                .collect(Collectors.toSet());
        if (memberIds.isEmpty()) return Collections.emptyList();

        // 🔹 Step 3: Fetch all submitted timesheets of these members
        List<TimeSheet> submittedSheets = timeSheetRepository.findSubmittedByUserIds(memberIds);
        if (submittedSheets.isEmpty()) return Collections.emptyList();

        // 🔹 Step 4: Fetch usernames from UMS (cache)
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

        // 🔹 Step 5: Group by userId and map weekly summary
        return submittedSheets.stream()
                .collect(Collectors.groupingBy(TimeSheet::getUserId))
                .entrySet().stream()
                .map(entry -> {
                    Long userId = entry.getKey();
                    List<TimeSheet> userSheets = entry.getValue();

                    List<WeekSummaryDTO> weekSummaries = userSheets.stream()
                            .collect(Collectors.groupingBy(ts -> ts.getWeekInfo().getWeekNo())) // group by weekNo (1–5)
                            .entrySet().stream()
                            .map(weekEntry -> {
                                Integer weekNo = weekEntry.getKey();
                                List<TimeSheet> weekSheets = weekEntry.getValue();
                                WeekInfo week = weekSheets.get(0).getWeekInfo(); // get start/end dates

                                List<TimeSheetSummaryDTO> timeSheetDTOs = weekSheets.stream()
                                        .map(this::mapToSummaryDTO)
                                        .toList();

                                BigDecimal totalHours = TimeUtil.sumHours(
                                        timeSheetDTOs.stream().map(TimeSheetSummaryDTO::getHoursWorked).toList()
                                );

                                WeekSummaryDTO weekDTO = new WeekSummaryDTO();
                                weekDTO.setWeekId(weekNo.longValue());             // ✅ use week number as weekId
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

    private TimeSheetSummaryDTO mapToSummaryDTO(TimeSheet ts) {
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
        dto.setStatus(ts.getStatus().name());
        dto.setEntries(entries);
        return dto;
    }
}
