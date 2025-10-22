package com.intranet.service;

import com.intranet.dto.*;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetReview;
import com.intranet.entity.WeekInfo;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.WeekInfoRepo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
public class WeeklySummaryService {

    private final TimeSheetRepo timeSheetRepo;
    private final WeekInfoRepo weekInfoRepo;

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

    // public WeeklySummaryDTO getWeeklySummary(Long userId) {
    // // 1️⃣ Find all weeks for the current month
    // LocalDate now = LocalDate.now();
    // LocalDate startOfMonth = now.withDayOfMonth(1);
    // LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
    // List<WeekInfo> weeks = weekInfoRepo
    //         .findByStartDateGreaterThanEqualAndEndDateLessThanEqualOrderByStartDateAsc(startOfMonth, endOfMonth);

    // // 2️⃣ Fetch timesheets for these weeks
    // List<Long> weekIds = weeks.stream().map(WeekInfo::getId).collect(Collectors.toList());
    // List<TimeSheet> timesheets = timeSheetRepo
    //         .findByUserIdAndWeekInfo_IdInOrderByWorkDateAsc(userId, weekIds);

    // // 🔄 Reverse the weeks to show last → first
    // Collections.reverse(weeks);

    // // 3️⃣ Map each week
    // List<WeekSummaryDTO> weeklySummary = weeks.stream().map(week -> {
    //     List<TimeSheetSummaryDTO> weekTimesheets = timesheets.stream()
    //             .filter(ts -> ts.getWeekInfo().getId().equals(week.getId()))
    //             .map(this::mapTimeSheetToSummaryDTO)
    //             .collect(Collectors.toList());

    //     WeekSummaryDTO weekDTO = new WeekSummaryDTO();
    //     weekDTO.setWeekId(week.getId());
    //     weekDTO.setStartDate(week.getStartDate());
    //     weekDTO.setEndDate(week.getEndDate());

    //     // ✅ Calculate total hours of this week (proper 01.67 format)
    //     BigDecimal totalHours = TimeUtil.sumHours(
    //             weekTimesheets.stream()
    //                     .map(TimeSheetSummaryDTO::getHoursWorked)
    //                     .collect(Collectors.toList())
    //     );
    //     weekDTO.setTotalHours(totalHours);
    //     weekDTO.setTimesheets(weekTimesheets);
    //     return weekDTO;
    // }).collect(Collectors.toList());

    // WeeklySummaryDTO summaryDTO = new WeeklySummaryDTO();
    // summaryDTO.setUserId(userId);
    // summaryDTO.setWeeklySummary(weeklySummary);

    // return summaryDTO;
    // }


    // private TimeSheetSummaryDTO mapTimeSheetToSummaryDTO(TimeSheet ts) {
    //     List<TimeSheetEntrySummaryDTO> entries = ts.getEntries().stream().map(e -> {
    //         TimeSheetEntrySummaryDTO dto = new TimeSheetEntrySummaryDTO();
    //         dto.setTimesheetEntryid(e.getId());
    //         dto.setProjectId(e.getProjectId());
    //         dto.setTaskId(e.getTaskId());
    //         dto.setDescription(e.getDescription());
    //         dto.setWorkLocation(e.getWorkLocation());
    //         dto.setFromTime(e.getFromTime() != null ? e.getFromTime().toLocalTime().toString() : null);
    //         dto.setToTime(e.getToTime() != null ? e.getToTime().toLocalTime().toString() : null);
    //         dto.setHoursWorked(e.getHoursWorked());
    //         dto.setOtherDescription(e.getOtherDescription());
    //         return dto;
    //     }).collect(Collectors.toList());

    //     TimeSheetSummaryDTO tsDTO = new TimeSheetSummaryDTO();
    //     tsDTO.setTimesheetId(ts.getId());
    //     tsDTO.setWorkDate(ts.getWorkDate());
    //     tsDTO.setHoursWorked(ts.getHoursWorked());
    //     tsDTO.setStatus(ts.getStatus().name());
    //     tsDTO.setEntries(entries);
    //     return tsDTO;
    // }

    @Value("${pms.api.base-url}")
    private String pmsBaseUrl;
    @Value("${ums.api.base-url}")
    private String umsBaseUrl;
     private final RestTemplate restTemplate = new RestTemplate();
    @Transactional
    public WeeklySummaryDTO getUserWeeklyTimeSheetHistory(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        List<WeekInfo> weeks = weekInfoRepo
                .findByStartDateGreaterThanEqualAndEndDateLessThanEqualOrderByStartDateAsc(startOfMonth, endOfMonth);

        List<Long> weekIds = weeks.stream().map(WeekInfo::getId).collect(Collectors.toList());
        List<TimeSheet> timesheets = timeSheetRepo.findByUserIdAndWeekInfo_IdInOrderByWorkDateAsc(userId, weekIds);

        if (timesheets.isEmpty()) {
            WeeklySummaryDTO empty = new WeeklySummaryDTO();
            empty.setUserId(userId);
            empty.setWeeklySummary(Collections.emptyList());
            return empty;
        }

        // 🔹 Step 1: Call PMS to get all projects
        String projectsUrl = String.format("%s/projects", pmsBaseUrl);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                projectsUrl,
                HttpMethod.GET,
                buildEntityWithAuth(),
                new ParameterizedTypeReference<>() {}
        );

        List<Map<String, Object>> projects = Optional.ofNullable(response.getBody()).orElse(Collections.emptyList());
        Map<Long, Map<String, Object>> projectMap = new HashMap<>();
        for (Map<String, Object> p : projects) {
            Long projectId = ((Number) p.get("id")).longValue();
            projectMap.put(projectId, p);
        }

        // 🔹 Step 2: Group by week and build DTOs
        List<WeekSummaryDTO> weeklySummaries = weeks.stream()
        .map(week -> buildWeekSummary(week, timesheets, projectMap))
        .sorted(Comparator.comparing(WeekSummaryDTO::getWeekId).reversed()) // week 5 → 1
        .collect(Collectors.toList());


        WeeklySummaryDTO summary = new WeeklySummaryDTO();
        summary.setUserId(userId);
        summary.setWeeklySummary(weeklySummaries);

        return summary;
    }

    private WeekSummaryDTO buildWeekSummary(WeekInfo week, List<TimeSheet> allSheets, Map<Long, Map<String, Object>> projectMap) {
        List<TimeSheet> weekSheets = allSheets.stream()
                .filter(ts -> ts.getWeekInfo().getId().equals(week.getId()))
                .collect(Collectors.toList());


        
        List<TimeSheetSummaryDTO> sheetDTOs = new ArrayList<>();

        for (TimeSheet ts : weekSheets) {
            List<ActionStatusDTO> actionStatusList = new ArrayList<>();

            ts.getEntries().forEach(entry -> {
                Map<String, Object> project = projectMap.get(entry.getProjectId());
                if (project == null) return;

                Map<String, Object> owner = (Map<String, Object>) project.get("owner");
                if (owner == null) return;

                Long managerId = ((Number) owner.get("id")).longValue();
                String managerName = (String) owner.get("name");

                Optional<TimeSheetReview> reviewOpt = ts.getReviews().stream()
                        .filter(r -> r.getManagerId().equals(managerId))
                        .findFirst();

                String action = reviewOpt.map(r -> r.getStatus().name()).orElse("Pending");

                // avoid duplicates
                boolean exists = actionStatusList.stream()
                        .anyMatch(a -> a.getApproverId().equals(managerId));
                if (!exists) {
                    actionStatusList.add(new ActionStatusDTO(managerId, managerName, action));
                }
            });

            // ✅ Determine overall status
            String overallStatus;
            boolean anyRejected = actionStatusList.stream().anyMatch(a -> "REJECTED".equalsIgnoreCase(a.getStatus()));
            boolean allApproved = !actionStatusList.isEmpty() && actionStatusList.stream().allMatch(a -> "APPROVED".equalsIgnoreCase(a.getStatus()));
            boolean anyApproved = actionStatusList.stream().anyMatch(a -> "APPROVED".equalsIgnoreCase(a.getStatus()));
            // boolean allPending = actionStatusList.stream().allMatch(a -> "PENDING".equalsIgnoreCase(a.getStatus()));

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
            }).collect(Collectors.toList());

            TimeSheetSummaryDTO tsDTO = new TimeSheetSummaryDTO();
            tsDTO.setTimesheetId(ts.getId());
            tsDTO.setWorkDate(ts.getWorkDate());
            tsDTO.setHoursWorked(ts.getHoursWorked());
            tsDTO.setEntries(entries);

            if (actionStatusList.isEmpty()) {
                overallStatus = "PENDING";
                actionStatusList.add(new ActionStatusDTO(99L, "Supervisor Mock", "PENDING"));
                    } else if (anyRejected) {
                        overallStatus = "REJECTED";
                    } else if (allApproved) {
                        overallStatus = "APPROVED";
                    } else if (anyApproved) {
                        overallStatus = "PARTIALLY APPROVED";
                    } else {
                        overallStatus = "PENDING";
                    }

                    tsDTO.setActionStatus(actionStatusList);
                    tsDTO.setStatus(overallStatus);
                    sheetDTOs.add(tsDTO);
            }

        // ✅ Calculate total hours
        BigDecimal totalHours = TimeUtil.sumHours(
                sheetDTOs.stream().map(TimeSheetSummaryDTO::getHoursWorked).collect(Collectors.toList())
        );

        WeekSummaryDTO weekDTO = new WeekSummaryDTO();
        weekDTO.setWeekId(week.getId());
        weekDTO.setStartDate(week.getStartDate());
        weekDTO.setEndDate(week.getEndDate());
        // 🔸 Validation: No timesheets found for this week
            if (weekSheets.isEmpty()) {
                weekDTO.setTimesheets(Collections.emptyList());
                weekDTO.setTotalHours(BigDecimal.ZERO);
                weekDTO.setWeeklyStatus("No Timesheets");
                return weekDTO;
            }
        else {
        weekDTO.setTotalHours(totalHours);
        weekDTO.setTimesheets(sheetDTOs);

        // Derive overall week-level status
        boolean anyRejected = sheetDTOs.stream().anyMatch(ts -> "Rejected".equalsIgnoreCase(ts.getStatus()));
        boolean allApproved = !sheetDTOs.isEmpty() && sheetDTOs.stream().allMatch(ts -> "Approved".equalsIgnoreCase(ts.getStatus()));
        boolean anyApproved = sheetDTOs.stream().anyMatch(ts -> "Partially Approved".equalsIgnoreCase(ts.getStatus()) || "Approved".equalsIgnoreCase(ts.getStatus()));
        
        

        if (anyRejected) weekDTO.setWeeklyStatus("Rejected");
        else if (allApproved) weekDTO.setWeeklyStatus("Approved");
        else if (anyApproved) weekDTO.setWeeklyStatus("Partially Approved");
        else weekDTO.setWeeklyStatus("Pending");

        return weekDTO;
        }
    }
    
}
