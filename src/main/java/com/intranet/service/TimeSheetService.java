package com.intranet.service;

import com.intranet.dto.AddEntryDTO;
import com.intranet.dto.TimeSheetEntryCreateDTO;
import com.intranet.dto.TimeSheetSummaryDTO;
import com.intranet.dto.TimeSheetEntrySummaryDTO;
import com.intranet.dto.WeekSummaryDTO;
import com.intranet.dto.external.ManagerUserMappingDTO;
import com.intranet.dto.external.ProjectTaskView;
import com.intranet.dto.external.ProjectWithUsersDTO;
import com.intranet.dto.external.TaskDTO;
import com.intranet.entity.InternalProject;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.entity.WeekInfo;
import com.intranet.repository.InternalProjectRepo;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeSheetService {

    private final TimeSheetRepo timeSheetRepository;
    private final WeekInfoRepo weekInfoRepository;
    private final InternalProjectRepo internalProjectRepository;

    @Transactional
    public String createTimeSheet(Long userId, LocalDate workDate, List<TimeSheetEntryCreateDTO> entriesDTO) {
        TimeSheet timeSheet = new TimeSheet();
        timeSheet.setUserId(userId);
        timeSheet.setWorkDate(workDate);
        timeSheet.setCreatedAt(LocalDateTime.now());
        timeSheet.setUpdatedAt(LocalDateTime.now());
        timeSheet.setStatus(TimeSheet.Status.DRAFT);

        WeekInfo weekInfo = findOrCreateWeekInfo(workDate);
        timeSheet.setWeekInfo(weekInfo);

        List<TimeSheetEntry> entries = entriesDTO.stream().map(dto -> {
            TimeSheetEntry entry = new TimeSheetEntry();
            entry.setTimeSheet(timeSheet);
            entry.setProjectId(dto.getProjectId());
            entry.setTaskId(dto.getTaskId());
            entry.setDescription(dto.getDescription());
            entry.setWorkLocation(dto.getWorkLocation());
            entry.setFromTime(dto.getFromTime());
            entry.setToTime(dto.getToTime());
            entry.setOtherDescription(dto.getOtherDescription());

            BigDecimal hours = dto.getHoursWorked();
            if (hours == null) {
                hours = calculateHours(dto.getFromTime(), dto.getToTime());
            }
            entry.setHoursWorked(hours);
            return entry;
        }).collect(Collectors.toList());

        timeSheet.setEntries(entries);
        timeSheet.setHoursWorked(entries.stream()
                .map(TimeSheetEntry::getHoursWorked)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        timeSheetRepository.save(timeSheet);

        return "Timesheet created successfully";
    }

    private BigDecimal calculateHours(LocalDateTime from, LocalDateTime to) {
    if (from == null || to == null) return BigDecimal.ZERO;
    if (to.isBefore(from)) throw new IllegalArgumentException("toTime cannot be before fromTime");

    Duration duration = Duration.between(from, to);
    long hours = duration.toHours();
    long minutes = duration.toMinutes() % 60;

    // Format as HH.MM where minutes are two digits
    String hhmm = String.format("%02d.%02d", hours, minutes);
    return new BigDecimal(hhmm);
    }


    private WeekInfo findOrCreateWeekInfo(LocalDate workDate) {
        return weekInfoRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(workDate, workDate)
                .orElseGet(() -> createWeekInfo(workDate));
    }

    private WeekInfo createWeekInfo(LocalDate date) {
        LocalDate startOfWeek = date.with(java.time.DayOfWeek.MONDAY);
        LocalDate endOfWeek = date.with(java.time.DayOfWeek.SUNDAY);

        WeekInfo weekInfo = new WeekInfo();
        weekInfo.setStartDate(startOfWeek);
        weekInfo.setEndDate(endOfWeek);
        weekInfo.setWeekNo(startOfWeek.get(WeekFields.ISO.weekOfYear()));
        weekInfo.setYear(startOfWeek.getYear());
        weekInfo.setMonth(startOfWeek.getMonthValue());
        weekInfo.setIncompleteWeek(false);

        return weekInfoRepository.save(weekInfo);
    }

    /**
     * Get timesheets grouped by week for a given date range
     */
    public List<WeekSummaryDTO> getTimesheetsByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        // Get all timesheets within the date range with weekInfo and entries eagerly loaded
        List<TimeSheet> timesheets = timeSheetRepository.findByWorkDateBetweenWithWeekInfoAndEntries(startDate, endDate);
        
        if (timesheets.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Get all week infos within the date range
        List<WeekInfo> weekInfos = weekInfoRepository.findByStartDateGreaterThanEqualAndEndDateLessThanEqualOrderByStartDateAsc(
            startDate, endDate);
        
        // Group timesheets by week - handle null weekInfo
        Map<Long, List<TimeSheet>> timesheetsByWeek = timesheets.stream()
            .filter(ts -> ts.getWeekInfo() != null) // Filter out timesheets without weekInfo
            .collect(Collectors.groupingBy(ts -> ts.getWeekInfo().getId()));
        
        // Create WeekSummaryDTO for each week
        List<WeekSummaryDTO> weekSummaries = new ArrayList<>();
        
        for (WeekInfo weekInfo : weekInfos) {
            List<TimeSheet> weekTimesheets = timesheetsByWeek.getOrDefault(weekInfo.getId(), new ArrayList<>());
            
            WeekSummaryDTO weekSummary = new WeekSummaryDTO();
            weekSummary.setWeekId(weekInfo.getId());
            weekSummary.setStartDate(weekInfo.getStartDate());
            weekSummary.setEndDate(weekInfo.getEndDate());
            
            // Calculate total hours for the week
            BigDecimal totalHours = weekTimesheets.stream()
                .map(TimeSheet::getHoursWorked)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            weekSummary.setTotalHours(totalHours);
            
            // Convert timesheets to DTOs
            List<TimeSheetSummaryDTO> timesheetSummaries = weekTimesheets.stream()
                .map(this::convertToTimeSheetSummaryDTO)
                .collect(Collectors.toList());
            weekSummary.setTimesheets(timesheetSummaries);
            
            weekSummaries.add(weekSummary);
        }
        
        // Sort by start date
        weekSummaries.sort(Comparator.comparing(WeekSummaryDTO::getStartDate));
        
        return weekSummaries;
    }
    
    private TimeSheetSummaryDTO convertToTimeSheetSummaryDTO(TimeSheet timesheet) {
        TimeSheetSummaryDTO summary = new TimeSheetSummaryDTO();
        summary.setTimesheetId(timesheet.getId());
        summary.setWorkDate(timesheet.getWorkDate());
        summary.setHoursWorked(timesheet.getHoursWorked());
        
        // Convert entries to DTOs
        List<TimeSheetEntrySummaryDTO> entrySummaries = timesheet.getEntries().stream()
            .map(this::convertToTimeSheetEntrySummaryDTO)
            .collect(Collectors.toList());
        summary.setEntries(entrySummaries);
        
        return summary;
    }
    
    private TimeSheetEntrySummaryDTO convertToTimeSheetEntrySummaryDTO(TimeSheetEntry entry) {
        TimeSheetEntrySummaryDTO summary = new TimeSheetEntrySummaryDTO();
        summary.setProjectId(entry.getProjectId());
        summary.setTaskId(entry.getTaskId());
        summary.setDescription(entry.getDescription());
        summary.setWorkLocation(entry.getWorkLocation());
        
        // Convert LocalDateTime to String format (HH:mm)
        if (entry.getFromTime() != null) {
            summary.setFromTime(entry.getFromTime().toLocalTime().toString());
        }
        if (entry.getToTime() != null) {
            summary.setToTime(entry.getToTime().toLocalTime().toString());
        }
        
        summary.setHoursWorked(entry.getHoursWorked());
        summary.setOtherDescription(entry.getOtherDescription());
        return summary;
    }
    
    /**
     * Debug method to get all timesheets in the database
     */
    public List<Map<String, Object>> debugGetAllTimesheets() {
        List<TimeSheet> allTimesheets = timeSheetRepository.findAll();
        
        return allTimesheets.stream().map(ts -> {
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("id", ts.getId());
            debugInfo.put("userId", ts.getUserId());
            debugInfo.put("workDate", ts.getWorkDate());
            debugInfo.put("hoursWorked", ts.getHoursWorked());
            debugInfo.put("status", ts.getStatus());
            debugInfo.put("weekInfoId", ts.getWeekInfo() != null ? ts.getWeekInfo().getId() : null);
            debugInfo.put("weekInfoStartDate", ts.getWeekInfo() != null ? ts.getWeekInfo().getStartDate() : null);
            debugInfo.put("weekInfoEndDate", ts.getWeekInfo() != null ? ts.getWeekInfo().getEndDate() : null);
            debugInfo.put("entriesCount", ts.getEntries() != null ? ts.getEntries().size() : 0);
            debugInfo.put("createdAt", ts.getCreatedAt());
            return debugInfo;
        }).collect(Collectors.toList());
    }

    public String addEntriesToTimeSheet(AddEntryDTO addEntryDTO) {
        Long timesheetId = addEntryDTO.getTimeSheetId();

        if (timesheetId == null) {
            return "timeSheetId is required in the request body";
        }

        Optional<TimeSheet> optionalTimeSheet = timeSheetRepository.findById(timesheetId);

        if (optionalTimeSheet.isEmpty()) {
            return "No timesheet found with id: " + timesheetId;
        }

        TimeSheet timeSheet = optionalTimeSheet.get();

        if (addEntryDTO.getEntries() == null || addEntryDTO.getEntries().isEmpty()) {
            return "No entries provided to add";
        }

        // Add each entry
        for (TimeSheetEntryCreateDTO entryDTO : addEntryDTO.getEntries()) {
            TimeSheetEntry entry = new TimeSheetEntry();
            entry.setTimeSheet(timeSheet);
            entry.setProjectId(entryDTO.getProjectId());
            entry.setTaskId(entryDTO.getTaskId());
            entry.setDescription(entryDTO.getDescription());
            entry.setFromTime(entryDTO.getFromTime());
            entry.setToTime(entryDTO.getToTime());
            entry.setWorkLocation(entryDTO.getWorkLocation());
            entry.setHoursWorked(entryDTO.getHoursWorked());
            entry.setOtherDescription(entryDTO.getOtherDescription());

            timeSheet.getEntries().add(entry);
        }

        // Recalculate total hours
        timeSheet.setHoursWorked(
            timeSheet.getEntries().stream()
                .map(e -> e.getHoursWorked() != null ? e.getHoursWorked() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
        );

        timeSheet.setUpdatedAt(LocalDateTime.now());

        timeSheetRepository.save(timeSheet);

        return "Entries added successfully. Total hours now: " + timeSheet.getHoursWorked();
    }

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${pms.api.base-url}")
    private String pmsBaseUrl;

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
    
    public List<ProjectTaskView> getUserTaskView(Long userId) {
        // 🔹 Step 1: Call PMS API dynamically using configured base URL
        String url = String.format("%s/tasks/assignee/%d", pmsBaseUrl, userId);

        ResponseEntity<List<Map<String, Object>>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        buildEntityWithAuth(),
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );

        List<Map<String, Object>> taskData = response.getBody();
        if (taskData == null || taskData.isEmpty()) {
            taskData = Collections.emptyList();
        }

        // 🔹 Step 2: Group external PMS tasks by projectId
        Map<Long, ProjectTaskView> projectMap = new LinkedHashMap<>();

        for (Map<String, Object> task : taskData) {
            Long taskId = task.get("id") != null ? ((Number) task.get("id")).longValue() : null;
            String taskName = (String) task.get("title");
            String description = (String) task.get("description");

            // Extract project info safely
            Map<String, Object> projectObj = (Map<String, Object>) task.get("project");
            Long projectId = null;
            final String projectName;

            if (projectObj != null) {
                Object idObj = projectObj.get("id");
                if (idObj instanceof Number) {
                    projectId = ((Number) idObj).longValue();
                }
                projectName = (String) projectObj.get("name");
            } else {
                projectName = null;
            }

            String startTime = task.get("startDate") != null ? task.get("startDate").toString() : null;
            String endTime = task.get("endDate") != null ? task.get("endDate").toString() : null;

            TaskDTO taskDTO = new TaskDTO(taskId, taskName, description, startTime, endTime);

            if (projectId != null) {
                projectMap
                    .computeIfAbsent(projectId, pid -> new ProjectTaskView(pid, projectName, new ArrayList<>()))
                    .getTasks()
                    .add(taskDTO);
            }
        }

        // 🔹 Step 3: Fetch Internal Projects from DB
        List<InternalProject> internalProjects = internalProjectRepository.findAll();
        Map<Long, ProjectTaskView> internalMap = new LinkedHashMap<>();

        for (InternalProject ip : internalProjects) {
            Long projectId = ip.getProjectId() != null ? ip.getProjectId().longValue() : null;
            Long taskId = ip.getTaskId() != null ? ip.getTaskId().longValue() : null;

            internalMap
                .computeIfAbsent(projectId != null ? projectId : 0L,
                        pid -> new ProjectTaskView(pid, ip.getProjectName(), new ArrayList<>()))
                .getTasks()
                .add(new TaskDTO(
                        taskId,
                        ip.getTaskName(),
                        null, // description
                        null, // startTime
                        null  // endTime
                ));
        }

        // 🔹 Step 4: Combine External (PMS) and Internal project-task data
        List<ProjectTaskView> combinedList = new ArrayList<>(projectMap.values());
        combinedList.addAll(internalMap.values());

        return combinedList;
        }
    
    
    public List<ProjectTaskView> getUserTaskViewM() {
    String url = String.format("%s/projects/projects-tasks", pmsBaseUrl);

    ResponseEntity<List<Map<String, Object>>> response =
            restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    buildEntityWithAuth(),
                    new ParameterizedTypeReference<>() {}
            );

    List<Map<String, Object>> projectData = response.getBody();
    if (projectData == null || projectData.isEmpty()) {
        return Collections.emptyList();
    }

    return projectData.stream().map(p -> {
        Long projectId = ((Number) p.get("projectId")).longValue();
        String projectName = (String) p.get("projectName");

        List<Map<String, Object>> taskList = (List<Map<String, Object>>) p.get("tasks");
        List<TaskDTO> tasks = taskList.stream()
                .map(t -> new TaskDTO(
                        ((Number) t.get("taskId")).longValue(),
                        (String) t.get("taskName"),
                        null, // description not available
                        null, // startTime not available
                        null  // endTime not available
                ))
                .toList();

        return new ProjectTaskView(projectId, projectName, tasks);
    }).toList();
    }
    public List<ManagerUserMappingDTO> getUsersAssignedToManagers(Long userId) {

    String url = String.format("%s/projects/member/%d", pmsBaseUrl, userId);
     // ✅ Use helper method to build HttpEntity with Authorization
    HttpEntity<Void> entity = buildEntityWithAuth();

    // Use exchange instead of getForEntity to pass headers
    ResponseEntity<List> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            List.class
    );

        if (response.getBody() == null) {
            return Collections.emptyList();
        }

        // Only keep projects where given user is a member
        List<Map<String, Object>> filteredProjects = ((List<Map<String, Object>>) response.getBody()).stream()
                .filter(p -> ((List<Map<String, Object>>) p.get("members")).stream()
                        .anyMatch(m -> Objects.equals(((Number) m.get("id")).longValue(), userId)))
                .toList();

        // Group by manager
        Map<Long, List<Map<String, Object>>> projectsByManager = filteredProjects.stream()
                .collect(Collectors.groupingBy(
                        p -> ((Number) ((Map<String, Object>) p.get("owner")).get("id")).longValue()
                ));

        // Build DTO
        return projectsByManager.entrySet().stream()
                .map(entry -> {
                    Long managerId = entry.getKey();
                    String managerName = (String) ((Map<String, Object>) entry.getValue().get(0).get("owner")).get("name");

                    List<ProjectWithUsersDTO> projects = entry.getValue().stream()
                            .map(p -> {
                                Long projectId = ((Number) p.get("id")).longValue();
                                String projectName = (String) p.get("name");

                                // Only this user in "users"
                                // List<UserSDTO> users = List.of(new UserSDTO(userId, "Ajay default"));

                                return new ProjectWithUsersDTO(projectId, projectName);
                            })
                            .toList();

                    return new ManagerUserMappingDTO(managerId, managerName, projects);
                })
                .toList();
    }
}