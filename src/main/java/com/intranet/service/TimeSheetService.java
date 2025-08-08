package com.intranet.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.intranet.dto.TimeSheetEntryDTO;
import com.intranet.dto.TimeSheetEntryResponseDTO;
import com.intranet.dto.TimeSheetEntryUpdateDTO;
import com.intranet.dto.TimeSheetResponseDTO;
import com.intranet.dto.TimeSheetUpdateRequestDTO;
import com.intranet.dto.external.ProjectTaskView;
import com.intranet.dto.external.TaskDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.repository.TimeSheetEntryRepo;
import com.intranet.repository.TimeSheetRepo;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TimeSheetService {

    @Autowired
    private TimeSheetRepo timeSheetRepository;

    @Autowired
    private TimeSheetEntryRepo timeSheetEntryRepository;
    
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

    return timesheets.stream().map(ts -> {
        TimeSheetResponseDTO dto = new TimeSheetResponseDTO();
        dto.setTimesheetId(ts.getTimesheetId());
        dto.setUserId(ts.getUserId());
        dto.setWorkDate(ts.getWorkDate());
        dto.setStatus(ts.getStatus());

        // Map entries
        List<TimeSheetEntryResponseDTO> entryDTOs = ts.getEntries().stream().map(entry -> {
            TimeSheetEntryResponseDTO entryDto = new TimeSheetEntryResponseDTO();
            entryDto.setTimesheetEntryId(entry.getTimesheetEntryId());
            entryDto.setProjectId(entry.getProjectId());
            entryDto.setTaskId(entry.getTaskId());
            entryDto.setDescription(entry.getDescription());
            entryDto.setWorkType(entry.getWorkType());
            entryDto.setFromTime(entry.getFromTime());
            entryDto.setToTime(entry.getToTime());  // Make sure it's getEndTime()
            entryDto.setHoursWorked(entry.getHoursWorked());
            entryDto.setOtherDescription(entry.getOtherDescription());
            return entryDto;
        }).toList();

        dto.setEntries(entryDTOs);
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

    // 🔄 Update status if provided
    if (dto.getStatus() != null) {
        timesheet.setStatus(dto.getStatus());
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

public List<ProjectTaskView> getUserTaskView(Long userId) {
        List<Map<String, Object>> ownedProjects = getMockOwnedProjects();
        List<Map<String, Object>> memberProjects = getMockMemberProjects();
        List<Map<String, Object>> tasks = getMockAssignedTasks();

        // Map projectId → project name
        Map<Long, String> projectIdToName = new HashMap<>();
        ownedProjects.forEach(proj -> projectIdToName.put(
            ((Number) proj.get("id")).longValue(),
            (String) proj.get("name"))
        );
        memberProjects.forEach(proj -> projectIdToName.put(
            ((Number) proj.get("id")).longValue(),
            (String) proj.get("name"))
        );

        // Group tasks by projectId
        Map<Long, List<TaskDTO>> projectIdToTasks = new HashMap<>();

        for (Map<String, Object> task : tasks) {
            Long taskId = ((Number) task.get("id")).longValue();
            String title = (String) task.get("title");
            String description = (String) task.get("description");

            Long projectId = ((Number) task.get("projectId")).longValue();
            String startTime = "";
            String endTime = "";

            Map<String, Object> sprint = (Map<String, Object>) task.get("sprint");
            if (sprint != null) {
                startTime = sprint.get("startDate").toString();
                endTime = sprint.get("endDate").toString();
            }

            TaskDTO dto = new TaskDTO(taskId, title, description, startTime, endTime);
            projectIdToTasks.computeIfAbsent(projectId, k -> new ArrayList<>()).add(dto);
        }

        // Final response construction
        List<ProjectTaskView> response = new ArrayList<>();
        for (Map.Entry<Long, List<TaskDTO>> entry : projectIdToTasks.entrySet()) {
            Long projectId = entry.getKey();
            String projectName = projectIdToName.getOrDefault(projectId, "Unknown Project");
            List<TaskDTO> taskList = entry.getValue();

            response.add(new ProjectTaskView(projectId, projectName, taskList));
        }

        return response;
    }

    // ------- MOCK DATA METHODS -------

    private List<Map<String, Object>> getMockOwnedProjects() {
        return Arrays.asList(
            mockProject(101L, "Tomo AI Platform"),
            mockProject(102L, "Internal Portal Revamp")
        );
    }

    private List<Map<String, Object>> getMockMemberProjects() {
        return Arrays.asList(
            mockProject(103L, "Website Redesign"),
            mockProject(104L, "Client Integration Layer")
        );
    }

    private List<Map<String, Object>> getMockAssignedTasks() {
    return Arrays.asList(
        // Project 101 - Tomo AI Platform
        mockTask(201L, "Define API schema", "Design all endpoint contracts", 101L, "2025-08-01", "2025-08-03"),
        mockTask(202L, "Setup DB models", "JPA entities and relationships", 101L, "2025-08-03", "2025-08-05"),
        mockTask(203L, "Auth flow", "JWT-based login/signup", 101L, "2025-08-06", "2025-08-08"),

        // Project 102 - Internal Portal Revamp
        mockTask(204L, "Design login screen", "React UI for login", 102L, "2025-08-02", "2025-08-05"),
        mockTask(205L, "Theme system", "Add Tailwind-based theming", 102L, "2025-08-06", "2025-08-07"),

        // Project 103 - Website Redesign
        mockTask(206L, "Frontend setup", "Initial project structure in Vite", 103L, "2025-08-04", "2025-08-06"),

        // Project 104 - Client Integration Layer
        mockTask(207L, "Webhook handler", "Handle incoming webhooks", 104L, "2025-08-03", "2025-08-08"),
        mockTask(208L, "API throttling", "Rate limit sensitive endpoints", 104L, "2025-08-09", "2025-08-11")
    );
}


    private Map<String, Object> mockProject(Long id, String name) {
        Map<String, Object> project = new HashMap<>();
        project.put("id", id);
        project.put("name", name);
        return project;
    }

    private Map<String, Object> mockTask(Long id, String title, String description, Long projectId, String startDate, String endDate) {
        Map<String, Object> task = new HashMap<>();
        task.put("id", id);
        task.put("title", title);
        task.put("description", description);
        task.put("projectId", projectId);

        Map<String, Object> sprint = new HashMap<>();
        sprint.put("startDate", startDate);
        sprint.put("endDate", endDate);
        task.put("sprint", sprint);

        return task;
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
            entryDTO.setFromTime(entry.getFromTime());
            entryDTO.setToTime(entry.getToTime());
            entryDTO.setHoursWorked(entry.getHoursWorked());
            entryDTO.setOtherDescription(entry.getOtherDescription());
            return entryDTO;
        }).collect(Collectors.toList());

        dto.setEntries(entryDTOs);

        return dto;
    }

}
