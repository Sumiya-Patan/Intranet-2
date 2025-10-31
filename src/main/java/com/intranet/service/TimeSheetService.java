package com.intranet.service;

import com.intranet.dto.AddEntryDTO;
import com.intranet.dto.DeleteTimeSheetEntriesRequest;
import com.intranet.dto.TimeSheetEntryCreateDTO;
import com.intranet.dto.TimeSheetUpdateRequest;
import com.intranet.dto.external.ManagerUserMappingDTO;
import com.intranet.dto.external.ProjectTaskView;
import com.intranet.dto.external.ProjectWithUsersDTO;
import com.intranet.dto.external.TaskDTO;
import com.intranet.entity.HolidayExcludeUsers;
import com.intranet.entity.InternalProject;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.entity.TimeSheetOnHolidays;
import com.intranet.entity.WeekInfo;
import com.intranet.repository.HolidayExcludeUsersRepo;
import com.intranet.repository.InternalProjectRepo;
import com.intranet.repository.TimeSheetEntryRepo;
import com.intranet.repository.TimeSheetOnHolidaysRepo;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.WeekInfoRepo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
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
    private final TimeSheetEntryRepo entryRepository;
    private final HolidayExcludeUsersRepo holidayExcludeUsersRepository;
    private final TimeSheetOnHolidaysRepo timeSheetOnHolidaysRepository;

    @Transactional
    public TimeSheet createTimeSheet(Long userId, LocalDate workDate, List<TimeSheetEntryCreateDTO> entriesDTO) {

        // Step 1: check if user has holiday exclusion
        Optional<HolidayExcludeUsers> excluded = holidayExcludeUsersRepository
            .findByUserIdAndHolidayDate(userId, workDate);

        //check if timesheet already exists for user on that date
        Optional<TimeSheet> existingTS = timeSheetRepository.findByUserIdAndWorkDate(userId, workDate);
        if (existingTS.isPresent()) {
          throw new IllegalArgumentException("Timesheet already exists for user on date: " + workDate);
        }

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
            entry.setBillable(dto.isBillable());

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

        // Step 3: if excluded present → create TimeSheetOnHolidays record
        if (excluded.isPresent()) {
            TimeSheetOnHolidays tsh = new TimeSheetOnHolidays();
            tsh.setTimeSheet(timeSheet);
            tsh.setHolidayExcludeUser(excluded.get());
            tsh.setIsHoliday(true);
            tsh.setHolidayDate(workDate.atStartOfDay());
            tsh.setDescription("Allowed to Submit on Holiday");
            timeSheetOnHolidaysRepository.save(tsh);
        }
        return timeSheet;
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
            entry.setBillable(entryDTO.isBillable());

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

    @Transactional
    public String deleteEntries(Long timesheetId,DeleteTimeSheetEntriesRequest request) {
        TimeSheet timeSheet = timeSheetRepository.findById(timesheetId)
                .orElseThrow(() -> new RuntimeException("TimeSheet not found with ID: " + timesheetId));

        // Filter entries that belong to this timesheet and need to be deleted
        List<TimeSheetEntry> entriesToDelete = timeSheet.getEntries().stream()
                .filter(e -> request.getEntryIds().contains(e.getId()))
                .toList();

        if (entriesToDelete.isEmpty()) {
            return "No matching entries found to delete.";
        }

        // Remove entries
        timeSheet.getEntries().removeAll(entriesToDelete);
        entryRepository.deleteAll(entriesToDelete);

        // If all entries deleted, delete timesheet itself
        if (timeSheet.getEntries().isEmpty()) {
            timeSheetRepository.delete(timeSheet);
            return "All entries deleted. TimeSheet also removed.";
        }

        // Recalculate total hours
        BigDecimal totalHours = timeSheet.getEntries().stream()
                .map(TimeSheetEntry::getHoursWorked)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        timeSheet.setHoursWorked(totalHours);

        timeSheetRepository.save(timeSheet);

        return "Selected entries deleted. Total hours now: " + totalHours;
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
            boolean isBillable = task.get("isBillable") != null && (Boolean) task.get("isBillable");

            TaskDTO taskDTO = new TaskDTO(taskId, taskName, description, startTime, endTime, isBillable);

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
                        null,  // endTime
                        ip.isBillable() // isBillable
                ));
        }

        // 🔹 Step 4: Combine External (PMS) and Internal project-task data
        List<ProjectTaskView> combinedList = new ArrayList<>(projectMap.values());
        combinedList.addAll(internalMap.values());

        return combinedList;
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

    public List<ProjectTaskView> getUserTaskViewM() {
        // 1️⃣ Fetch from PMS
        String url = String.format("%s/projects/projects-tasks", pmsBaseUrl);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                buildEntityWithAuth(),
                new ParameterizedTypeReference<>() {}
        );

        List<Map<String, Object>> pmsData = response.getBody();
        if (pmsData == null) pmsData = new ArrayList<>();

        // 2️⃣ Fetch from internal DB
        List<InternalProject> internalProjects = internalProjectRepository.findAll();

        // 3️⃣ Convert PMS projects into DTOs
        Map<Long, ProjectTaskView> mergedProjects = new LinkedHashMap<>();

        for (Map<String, Object> p : pmsData) {
            Long projectId = ((Number) p.get("projectId")).longValue();
            final String[] projectNameHolder = new String[] { (String) p.get("projectName") };

            List<Map<String, Object>> taskList = (List<Map<String, Object>>) p.get("tasks");

            List<TaskDTO> tasks = taskList.stream().map(t -> {
                Long taskId = ((Number) t.get("taskId")).longValue();
                final String[] taskNameHolder = new String[] { (String) t.get("taskName") };
                Boolean isBillable = t.get("isBillable") != null && (Boolean) t.get("isBillable");

                return new TaskDTO(taskId, taskNameHolder[0], null, null, null, isBillable);
            }).collect(Collectors.toList());

            // Override project name if found internally
            internalProjects.stream()
                    .filter(ip -> ip.getProjectId().equals(projectId.intValue()))
                    .findFirst()
                    .ifPresent(ip -> {
                        if (ip.getProjectName() != null) projectNameHolder[0] = ip.getProjectName();
                    });

            mergedProjects.put(projectId, new ProjectTaskView(projectId, projectNameHolder[0], tasks));
        }

        // 4️⃣ Add internal-only projects (not present in PMS)
        Map<Integer, List<InternalProject>> groupedInternal =
                internalProjects.stream().collect(Collectors.groupingBy(InternalProject::getProjectId));

        for (Map.Entry<Integer, List<InternalProject>> entry : groupedInternal.entrySet()) {
            Long internalProjId = entry.getKey().longValue();
            if (!mergedProjects.containsKey(internalProjId)) {
                List<TaskDTO> tasks = entry.getValue().stream()
                        .map(ip -> new TaskDTO(
                                ip.getTaskId().longValue(),
                                ip.getTaskName(),
                                null, null, null, ip.isBillable()))
                        .collect(Collectors.toList());

                InternalProject first = entry.getValue().get(0);
                mergedProjects.put(internalProjId, new ProjectTaskView(
                        internalProjId,
                        first.getProjectName(),
                        tasks
                ));
            }
        }

        // 5️⃣ Return combined sorted list
        return new ArrayList<>(mergedProjects.values());
    }

    @Autowired
    private TimeSheetEntryRepo timeSheetEntryRepository;
    @Transactional
    public String updateEntries(Long timesheetId,TimeSheetUpdateRequest request) {

    TimeSheet timeSheet = timeSheetRepository.findById(timesheetId)
            .orElseThrow(() -> new RuntimeException("TimeSheet not found with ID: " + timesheetId));

    BigDecimal totalHours = BigDecimal.ZERO;

    for (TimeSheetUpdateRequest.EntryUpdateDto dto : request.getEntries()) {
        TimeSheetEntry entry = timeSheetEntryRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Entry not found with ID: " + dto.getId()));

        // partial update — only update fields that are provided
        if (dto.getProjectId() != null) entry.setProjectId(dto.getProjectId());
        if (dto.getTaskId() != null) entry.setTaskId(dto.getTaskId());
        if (dto.getDescription() != null) entry.setDescription(dto.getDescription());
        if (dto.getWorkLocation() != null) entry.setWorkLocation(dto.getWorkLocation());
        if (dto.getFromTime() != null) entry.setFromTime(dto.getFromTime());
        if (dto.getToTime() != null) entry.setToTime(dto.getToTime());
        if (dto.getOtherDescription() != null) entry.setOtherDescription(dto.getOtherDescription());
        if (dto.getHoursWorked() != null)
            entry.setHoursWorked(BigDecimal.valueOf(dto.getHoursWorked()));
        if (dto.getIsBillable() != null)
            entry.setBillable(dto.getIsBillable());

        timeSheetEntryRepository.save(entry);
    }

    // Recalculate total hours after update
    for (TimeSheetEntry e : timeSheet.getEntries()) {
        if (e.getHoursWorked() != null) {
            totalHours = totalHours.add(e.getHoursWorked());
        }
    }

    timeSheet.setHoursWorked(totalHours);
    timeSheet.setUpdatedAt(LocalDateTime.now());
    timeSheetRepository.save(timeSheet);

    return "Entries updated successfully. Total hours now: " + totalHours.stripTrailingZeros().toPlainString();
}

    
}


