package com.intranet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

import com.intranet.dto.ActionStatusDTO;
import com.intranet.dto.TimeSheetEntryCreateRequestDTO;
import com.intranet.dto.TimeSheetEntryDTO;
import com.intranet.dto.TimeSheetEntryResponseDTO;
import com.intranet.dto.TimeSheetEntryUpdateDTO;
import com.intranet.dto.TimeSheetResponseDTO;
import com.intranet.dto.TimeSheetUpdateRequestDTO;
import com.intranet.dto.UserSDTO;
import com.intranet.dto.external.ManagerUserMappingDTO;
import com.intranet.dto.external.ProjectTaskView;
import com.intranet.dto.external.ProjectWithUsersDTO;
import com.intranet.dto.external.TaskDTO;
import com.intranet.entity.InternalProject;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.entity.TimeSheetReview;
import com.intranet.repository.InternalProjectRepo;
import com.intranet.repository.TimeSheetEntryRepo;
import com.intranet.repository.TimeSheetRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TimeSheetService {

    @Autowired
    private TimeSheetRepo timeSheetRepository;

    @Autowired
    private TimeSheetEntryRepo timeSheetEntryRepository;

    @Autowired
    private InternalProjectRepo internalProjectRepository;
    
  public void createTimeSheetWithEntries(
        Long userId,
        LocalDate workDate,
        List<TimeSheetEntryDTO> entriesDto) {

    // 🔹 Step 1: Calculate hours if from/to time is provided
    for (TimeSheetEntryDTO dto : entriesDto) {
        if (dto.getFromTime() != null && dto.getToTime() != null) {
            long minutes = Duration.between(dto.getFromTime(), dto.getToTime()).toMinutes();
            dto.setHoursWorked(BigDecimal.valueOf(minutes / 60.0));
        } else if (dto.getHoursWorked() == null) {
            dto.setHoursWorked(BigDecimal.ZERO);
        }
    }

    // 🔹 Step 2: Create TimeSheet
    TimeSheet timesheet = new TimeSheet();
    timesheet.setUserId(userId);
    timesheet.setWorkDate(workDate);

    // 🔹 Step 3: Create and attach entries
    List<TimeSheetEntry> entries = new ArrayList<>();
    for (TimeSheetEntryDTO dto : entriesDto) {
        TimeSheetEntry entry = new TimeSheetEntry();
        entry.setTimeSheet(timesheet);
        entry.setProjectId(dto.getProjectId());
        entry.setTaskId(dto.getTaskId());
        entry.setDescription(dto.getDescription());
        // entry.setWorkLocation(dto.getWorkLocation());
        entry.setIsBillable(dto.getIsBillable());
        entry.setWorkType(dto.getWorkType());
        entry.setFromTime(dto.getFromTime());
        entry.setToTime(dto.getToTime());
        entry.setHoursWorked(dto.getHoursWorked());
        entry.setOtherDescription(dto.getOtherDescription());

        entries.add(entry);
    }
    timesheet.setEntries(entries);

    timeSheetRepository.save(timesheet);
    }
    
    public List<TimeSheetResponseDTO> getUserTimeSheetHistory(Long userId) {
    List<TimeSheet> timesheets = timeSheetRepository.findByUserIdOrderByWorkDateDesc(userId);
    if (timesheets.isEmpty()) return Collections.emptyList();

    // ✅ Step 1: Fetch internal projects
    List<InternalProject> internalProjects = internalProjectRepository.findAll();
    Map<Long, Map<String, Object>> combinedProjectMap = new HashMap<>();

    for (InternalProject ip : internalProjects) {
        Map<String, Object> internalMap = new HashMap<>();
        internalMap.put("id", ip.getProjectId());
        internalMap.put("name", ip.getProjectName());
        combinedProjectMap.put(ip.getProjectId().longValue(), internalMap);
    }

    // ✅ Step 2: Fetch PMS projects
    String projectsUrl = String.format("%s/projects", pmsBaseUrl);
    ResponseEntity<List<Map<String, Object>>> projectResponse = restTemplate.exchange(
            projectsUrl,
            HttpMethod.GET,
            buildEntityWithAuth(),
            new ParameterizedTypeReference<>() {}
    );
    List<Map<String, Object>> projects = projectResponse.getBody();

    if (projects != null) {
        for (Map<String, Object> p : projects) {
            Long projectId = ((Number) p.get("id")).longValue();
            combinedProjectMap.putIfAbsent(projectId, p);
        }
    }

    // ✅ Step 3: Map timesheets → DTOs
    return timesheets.stream().map(ts -> {
        TimeSheetResponseDTO dto = new TimeSheetResponseDTO();
        dto.setTimesheetId(ts.getTimesheetId());
        dto.setUserId(ts.getUserId());
        dto.setWorkDate(ts.getWorkDate());

        // ✅ Map entries
        List<TimeSheetEntryResponseDTO> entries = ts.getEntries().stream().map(entry -> {
            TimeSheetEntryResponseDTO entryDto = new TimeSheetEntryResponseDTO();
            entryDto.setTimesheetEntryId(entry.getTimesheetEntryId());
            entryDto.setProjectId(entry.getProjectId());
            entryDto.setTaskId(entry.getTaskId());
            entryDto.setDescription(entry.getDescription());
            entryDto.setWorkType(entry.getWorkType());
            entryDto.setIsBillable(entry.getIsBillable());
            entryDto.setFromTime(entry.getFromTime());
            entryDto.setToTime(entry.getToTime());
            entryDto.setHoursWorked(entry.getHoursWorked());
            entryDto.setOtherDescription(entry.getOtherDescription());
            return entryDto;
        }).toList();
        dto.setEntries(entries);

        // ✅ Step 4: Build actionStatus list (manager or mock)
        List<ActionStatusDTO> actionStatusList = new ArrayList<>();
        for (TimeSheetEntry entry : ts.getEntries()) {
            Map<String, Object> project = combinedProjectMap.get(entry.getProjectId());
            if (project == null) continue;

            Map<String, Object> owner = (Map<String, Object>) project.get("owner");
            if (owner == null) continue;

            Long managerId = ((Number) owner.get("id")).longValue();
            String managerName = (String) owner.get("name");

            Optional<TimeSheetReview> reviewOpt = ts.getReviews().stream()
                    .filter(r -> r.getManagerId().equals(managerId))
                    .findFirst();

            String action = reviewOpt.map(TimeSheetReview::getAction).orElse("Pending");

            boolean exists = actionStatusList.stream()
                    .anyMatch(a -> a.getApproverId().equals(managerId));
            if (!exists) {
                actionStatusList.add(new ActionStatusDTO(managerId, managerName, action));
            }
        }

        // ✅ Step 5: Add mock supervisor if no approvers
        if (actionStatusList.isEmpty()) {
            actionStatusList.add(new ActionStatusDTO(1L, "Supervisor Mock", "Pending"));
            dto.setStatus("Pending");
        } else {
            boolean anyRejected = actionStatusList.stream().anyMatch(a -> "Rejected".equalsIgnoreCase(a.getStatus()));
            boolean allApproved = actionStatusList.stream().allMatch(a -> "Approved".equalsIgnoreCase(a.getStatus()));
            boolean anyApproved = actionStatusList.stream().anyMatch(a -> "Approved".equalsIgnoreCase(a.getStatus()));
            boolean allPending = actionStatusList.stream().allMatch(a -> "Pending".equalsIgnoreCase(a.getStatus()));

            if (anyRejected) dto.setStatus("Rejected");
            else if (allApproved) dto.setStatus("Approved");
            else if (anyApproved) dto.setStatus("Partially Approved");
            else if (allPending) dto.setStatus("Pending");
            else dto.setStatus("Pending");
        }

        dto.setActionStatus(actionStatusList);
        return dto;
    }).toList();
    }


    public List<TimeSheetResponseDTO> getUserTimeSheetHistoryByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
    List<TimeSheet> timesheets = timeSheetRepository
            .findByUserIdAndWorkDateBetweenOrderByWorkDateDesc(userId, startDate, endDate);

    if (timesheets.isEmpty()) return Collections.emptyList();

    // ✅ Fetch projects
    String projectsUrl = String.format("%s/projects", pmsBaseUrl);
    ResponseEntity<List<Map<String, Object>>> projectResponse = restTemplate.exchange(
            projectsUrl,
            HttpMethod.GET,
            buildEntityWithAuth(),
            new ParameterizedTypeReference<>() {}
    );
    List<Map<String, Object>> projects = projectResponse.getBody();

    final Map<Long, Map<String, Object>> projectMap = (projects != null)
            ? projects.stream().collect(Collectors.toMap(
                p -> ((Number) p.get("id")).longValue(),
                p -> p
            ))
            : new HashMap<>();

    return timesheets.stream().map(ts -> {
        TimeSheetResponseDTO dto = new TimeSheetResponseDTO();
        dto.setTimesheetId(ts.getTimesheetId());
        dto.setUserId(ts.getUserId());
        dto.setWorkDate(ts.getWorkDate());

        // ✅ Map entries
        List<TimeSheetEntryResponseDTO> entries = ts.getEntries().stream().map(entry -> {
            TimeSheetEntryResponseDTO entryDto = new TimeSheetEntryResponseDTO();
            entryDto.setTimesheetEntryId(entry.getTimesheetEntryId());
            entryDto.setProjectId(entry.getProjectId());
            entryDto.setTaskId(entry.getTaskId());
            entryDto.setDescription(entry.getDescription());
            entryDto.setWorkType(entry.getWorkType());
            entryDto.setIsBillable(entry.getIsBillable());
            entryDto.setFromTime(entry.getFromTime());
            entryDto.setToTime(entry.getToTime());
            entryDto.setHoursWorked(entry.getHoursWorked());
            entryDto.setOtherDescription(entry.getOtherDescription());
            return entryDto;
        }).toList();
        dto.setEntries(entries);

        // ✅ Build action status
        List<ActionStatusDTO> actionStatusList = new ArrayList<>();
        for (TimeSheetEntry entry : ts.getEntries()) {
            Map<String, Object> project = projectMap.get(entry.getProjectId());
            if (project == null) continue;

            Map<String, Object> owner = (Map<String, Object>) project.get("owner");
            if (owner == null) continue;

            Long managerId = ((Number) owner.get("id")).longValue();
            String managerName = (String) owner.get("name");

            Optional<TimeSheetReview> reviewOpt = ts.getReviews().stream()
                    .filter(r -> r.getManagerId().equals(managerId))
                    .findFirst();

            String action = reviewOpt.map(TimeSheetReview::getAction).orElse("Pending");

            boolean exists = actionStatusList.stream()
                    .anyMatch(a -> a.getApproverId().equals(managerId));
            if (!exists) {
                actionStatusList.add(new ActionStatusDTO(managerId, managerName, action));
            }
        }

        // ✅ Add mock supervisor if empty
        if (actionStatusList.isEmpty()) {
            actionStatusList.add(new ActionStatusDTO(1L, "Supervisor Mock", "Pending"));
            dto.setStatus("Pending");
        } else {
            boolean anyRejected = actionStatusList.stream().anyMatch(a -> "Rejected".equalsIgnoreCase(a.getStatus()));
            boolean allApproved = actionStatusList.stream().allMatch(a -> "Approved".equalsIgnoreCase(a.getStatus()));
            boolean anyApproved = actionStatusList.stream().anyMatch(a -> "Approved".equalsIgnoreCase(a.getStatus()));
            boolean allPending = actionStatusList.stream().allMatch(a -> "Pending".equalsIgnoreCase(a.getStatus()));

            if (anyRejected) dto.setStatus("Rejected");
            else if (allApproved) dto.setStatus("Approved");
            else if (anyApproved) dto.setStatus("Partially Approved");
            else if (allPending) dto.setStatus("Pending");
            else dto.setStatus("Pending");
        }

        dto.setActionStatus(actionStatusList);
        return dto;
    }).toList();
    }


    @Transactional
    public void partialUpdateTimesheet(Long timesheetId, TimeSheetUpdateRequestDTO dto) {
    TimeSheet timesheet = timeSheetRepository.findById(timesheetId)
            .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

    // 🗓 Update workDate if provided
    if (dto.getWorkDate() != null) {
        timesheet.setWorkDate(dto.getWorkDate());
    }

    // 🔄 Update entries
    if (dto.getEntries() != null && !dto.getEntries().isEmpty()) {
        for (TimeSheetEntryUpdateDTO entryDto : dto.getEntries()) {
            TimeSheetEntry entry = timeSheetEntryRepository.findById(entryDto.getTimesheetEntryId())
                    .orElseThrow(() -> new IllegalArgumentException("Entry not found with ID: " + entryDto.getTimesheetEntryId()));

            if (entryDto.getProjectId() != null) entry.setProjectId(entryDto.getProjectId());
            if (entryDto.getTaskId() != null) entry.setTaskId(entryDto.getTaskId());
            if (entryDto.getDescription() != null) entry.setDescription(entryDto.getDescription());
            if (entryDto.getWorkType() != null) entry.setWorkType(entryDto.getWorkType());
            // if (entryDto.getWorkLocation() != null) entry.setWorkLocation(entryDto.getWorkLocation());
            if (entryDto.getIsBillable() != null) entry.setIsBillable(entryDto.getIsBillable());
            if (entryDto.getFromTime() != null) entry.setFromTime(entryDto.getFromTime());
            if (entryDto.getToTime() != null) entry.setToTime(entryDto.getToTime());
            if (entryDto.getOtherDescription() != null) entry.setOtherDescription(entryDto.getOtherDescription());

            // 🕒 Calculate hours if both from/to time provided
            if (entryDto.getFromTime() != null && entryDto.getToTime() != null) {
                long minutes = Duration.between(entryDto.getFromTime(), entryDto.getToTime()).toMinutes();
                entry.setHoursWorked(BigDecimal.valueOf(minutes / 60.0));
            } else if (entryDto.getHoursWorked() != null) {
                entry.setHoursWorked(entryDto.getHoursWorked());
            }

            timeSheetEntryRepository.save(entry);
        }
    }
    timesheet.setUpdatedAt(LocalDateTime.now());
    timeSheetRepository.save(timesheet);
}
        
    public TimeSheetResponseDTO getTimeSheetById(Long id) {
        TimeSheet timeSheet = timeSheetRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Timesheet not found with id: " + id));

        TimeSheetResponseDTO dto = new TimeSheetResponseDTO();
        dto.setTimesheetId(timeSheet.getTimesheetId());
        dto.setUserId(timeSheet.getUserId());
        dto.setWorkDate(timeSheet.getWorkDate());
        dto.setStatus(timeSheet.getStatus());

        List<TimeSheetEntryResponseDTO> entryDTOs = timeSheet.getEntries().stream().map(entry -> {
            TimeSheetEntryResponseDTO entryDTO = new TimeSheetEntryResponseDTO();
            entryDTO.setTimesheetEntryId(entry.getTimesheetEntryId());
            entryDTO.setProjectId(entry.getProjectId());
            entryDTO.setTaskId(entry.getTaskId());
            entryDTO.setDescription(entry.getDescription());
            entryDTO.setWorkType(entry.getWorkType());
            // entryDTO.setWorkLocation(entry.getWorkLocation());
            entryDTO.setIsBillable(entry.getIsBillable());
            entryDTO.setFromTime(entry.getFromTime());
            entryDTO.setToTime(entry.getToTime());
            if (entryDTO.getFromTime() != null && entryDTO.getToTime() != null) {
            long minutes = Duration.between(entryDTO.getFromTime(), entryDTO.getToTime()).toMinutes();
            entryDTO.setHoursWorked(BigDecimal.valueOf(minutes / 60.0));
        } else if (entryDTO.getHoursWorked() == null) {
            entryDTO.setHoursWorked(BigDecimal.ZERO);
        }
            entryDTO.setOtherDescription(entry.getOtherDescription());
            return entryDTO;
        }).collect(Collectors.toList());

        dto.setEntries(entryDTOs);

        return dto;
    }


    public ResponseEntity<String> addEntriesToTimeSheet(Long timesheetId, List<TimeSheetEntryCreateRequestDTO> newEntries) {
    Optional<TimeSheet> optional = timeSheetRepository.findById(timesheetId);
    if (optional.isEmpty()) {
        return ResponseEntity.badRequest().body("Timesheet not found");
    }

    TimeSheet timeSheet = optional.get();
    if (timeSheet.getStatus().equalsIgnoreCase("Approved")) {    
        return ResponseEntity.badRequest().body("Cannot add entries to an approved timesheet");
    }

    List<TimeSheetEntry> existingEntries = timeSheet.getEntries();

    for (TimeSheetEntryCreateRequestDTO dto : newEntries) {
        if (dto.getFromTime() == null || dto.getToTime() == null || dto.getToTime().isBefore(dto.getFromTime())) {
            return ResponseEntity.badRequest().body("Invalid fromTime/toTime for entry");
        }

        // // Check overlap with existing entries
        // boolean overlapsExisting = existingEntries.stream().anyMatch(e ->
        //     timesOverlap(e.getFromTime(), e.getToTime(), dto.getFromTime(), dto.getToTime())
        // );
        // if (overlapsExisting) {
        //     return ResponseEntity.badRequest().body("New entry overlaps with an existing entry");
        // }

        // Check overlap with other new entries in batch
        boolean overlapsNewEntries = newEntries.stream()
                .filter(other -> other != dto)
                .anyMatch(other -> timesOverlap(other.getFromTime(), other.getToTime(), dto.getFromTime(), dto.getToTime()));
        if (overlapsNewEntries) {
            return ResponseEntity.badRequest().body("New entries in the batch overlap with each other");
        }

        // Create entry
        TimeSheetEntry entry = new TimeSheetEntry();
        entry.setTimeSheet(timeSheet);
        entry.setProjectId(dto.getProjectId());
        entry.setTaskId(dto.getTaskId());
        entry.setDescription(dto.getDescription());
        entry.setWorkType(dto.getWorkType());
        entry.setIsBillable(dto.getIsBillable());
        entry.setFromTime(dto.getFromTime());
        entry.setToTime(dto.getToTime());

        // ✅ Convert duration to HH.mm format
        Duration duration = Duration.between(dto.getFromTime(), dto.getToTime());
        long hoursPart = duration.toHours();
        long minutesPart = duration.toMinutes() % 60;

        BigDecimal hoursWorked = new BigDecimal(hoursPart + "." + String.format("%02d", minutesPart));
        entry.setHoursWorked(hoursWorked);

        entry.setOtherDescription(dto.getOtherDescription());
        timeSheet.getEntries().add(entry);
    }

    timeSheet.setUpdatedAt(LocalDateTime.now());
    timeSheet.setStatus("Pending");
    timeSheetRepository.save(timeSheet);

    return ResponseEntity.ok("Entries added successfully");
    }

    /** Returns true if two time intervals overlap. */
    private boolean timesOverlap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
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
                                List<UserSDTO> users = List.of(new UserSDTO(userId, "Ajay default"));

                                return new ProjectWithUsersDTO(projectId, projectName, users);
                            })
                            .toList();

                    return new ManagerUserMappingDTO(managerId, managerName, projects);
                })
                .toList();
    }

    public Map<String, Object> getSummary(Long userId, LocalDate startDate, LocalDate endDate) {

    // ✅ 0. Default date logic
    LocalDate today = LocalDate.now();
    if (startDate == null) {
        startDate = today.withDayOfMonth(1); // First day of current month
    }
    if (endDate == null) {
        endDate = today; // Current day
    }

    HttpEntity<Void> entity = buildEntityWithAuth();

    // ✅ 1. Fetch projects from PMS
    String url = pmsBaseUrl + "/projects";
    ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<>() {}
    );

    Map<Long, String> projectMap = Optional.ofNullable(response.getBody())
            .orElse(Collections.emptyList())
            .stream()
            .collect(Collectors.toMap(
                    p -> ((Number) p.get("id")).longValue(),
                    p -> (String) p.get("name")
            ));

    // ✅ 2. Fetch timesheets for user in range
    List<TimeSheet> sheets = timeSheetRepository.findByUserIdAndWorkDateBetween(userId, startDate, endDate);

    // Flatten entries
    List<TimeSheetEntry> entries = sheets.stream()
            .flatMap(s -> s.getEntries().stream())
            .toList();

    // ✅ 3. Timesheet Summary
    Map<String, Long> timesheetSummary = Map.of(
            "pending", sheets.stream().filter(s -> "Pending".equalsIgnoreCase(s.getStatus())).count(),
            "approved", sheets.stream().filter(s -> "Approved".equalsIgnoreCase(s.getStatus())).count(),
            "rejected", sheets.stream().filter(s -> "Rejected".equalsIgnoreCase(s.getStatus())).count()
    );

    // ✅ 4. Billable Activity
    long billableLogs = entries.stream().filter(e -> Boolean.TRUE.equals(e.getIsBillable())).count();
    long nonBillableLogs = entries.size() - billableLogs;
    Map<String, Long> billableActivity = Map.of(
            "billableLogs", billableLogs,
            "nonBillableLogs", nonBillableLogs
    );

    // ✅ 5. Project Summary (using true HH.mm formatting)
    Map<Long, BigDecimal> projectHours = entries.stream()
            .collect(Collectors.groupingBy(
                    TimeSheetEntry::getProjectId,
                    Collectors.reducing(
                            BigDecimal.ZERO,
                            this::calculateHours,
                            BigDecimal::add
                    )
            ));

    List<Map<String, Object>> projectSummary = projectHours.entrySet().stream()
            .map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("projectId", e.getKey());
                map.put("projectName", projectMap.getOrDefault(e.getKey(), "Unknown Project"));
                map.put("totalHoursWorked", formatHours(e.getValue())); // fixed
                return map;
            })
            .collect(Collectors.toList());

    // ✅ 6. Weekly Summary
    Map<String, BigDecimal> weeklySummary = Arrays.stream(DayOfWeek.values())
            .collect(Collectors.toMap(
                    d -> d.name().toLowerCase(),
                    d -> BigDecimal.ZERO,
                    (a, b) -> a,
                    LinkedHashMap::new
            ));

    entries.forEach(entry -> {
        LocalDate workDate = entry.getTimeSheet().getWorkDate();
        DayOfWeek day = workDate.getDayOfWeek();
        BigDecimal hours = calculateHours(entry);

        weeklySummary.merge(day.name().toLowerCase(), hours, BigDecimal::add);
    });

    // ✅ Round weekly hours & format to 2 decimals
    weeklySummary.replaceAll((k, v) -> formatHours(v));

    // ✅ 7. Calculate total hours worked for range
    BigDecimal totalHours = entries.stream()
            .map(this::calculateHours)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    totalHours = formatHours(totalHours);

    // ✅ 8. Final Response
    return Map.of(
            "timesheetSummary", timesheetSummary,
            "billableActivity", billableActivity,
            "projectSummary", projectSummary,
            "weeklySummary", weeklySummary,
            "totalHours", totalHours,
            "dateRange", Map.of("startDate", startDate, "endDate", endDate)
    );
    }


    private BigDecimal calculateHours(TimeSheetEntry entry) {
    if (entry.getFromTime() != null && entry.getToTime() != null) {
        long totalMinutes = Duration.between(entry.getFromTime(), entry.getToTime()).toMinutes();
        long hoursPart = totalMinutes / 60;
        long minutesPart = totalMinutes % 60;

        // Convert 5h 40m → 5.40 (HH.mm format for readability)
        String formatted = String.format("%d.%02d", hoursPart, minutesPart);
        return new BigDecimal(formatted);
    } else if (entry.getHoursWorked() != null) {
        return entry.getHoursWorked();
    }
    return BigDecimal.ZERO;
    }

    private BigDecimal formatHours(BigDecimal hoursDecimal) {
    // Convert to minutes total
    long totalMinutes = hoursDecimal.multiply(BigDecimal.valueOf(60)).longValue();
    long hours = totalMinutes / 60;
    long minutes = totalMinutes % 60;

    return new BigDecimal(String.format("%d.%02d", hours, minutes));
    }

}
