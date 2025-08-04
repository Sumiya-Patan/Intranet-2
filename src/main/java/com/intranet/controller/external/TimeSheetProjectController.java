package com.intranet.controller.external;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.UserSDTO;
import com.intranet.dto.external.ManagerInfoDTO;
import com.intranet.dto.external.ManagerUserMappingDTO;
import com.intranet.dto.external.ProjectManagerInfoDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.repository.TimeSheetEntryRepo;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.service.external.ExternalProjectApiService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/timesheet/project-info")
@RequiredArgsConstructor
public class TimeSheetProjectController {

    @Autowired
    private final TimeSheetEntryRepo timeSheetEntryRepository;

    @Autowired
    private final TimeSheetRepo timeSheetRepository;

    @Autowired
    private final ExternalProjectApiService externalProjectApiService;

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectManagerInfoDTO>> getProjectAndManagerInfoFromTimesheets() {

        // 1. Get all project IDs used in timesheet entries
        Set<Long> projectIds = timeSheetEntryRepository.findAll()
            .stream()
            .map(TimeSheetEntry::getProjectId)
            .collect(Collectors.toSet());

        // 2. For each projectId, call external API (mocked) to get project & manager info
        List<ProjectManagerInfoDTO> result = projectIds.stream()
            .map(pid -> {
                String name = externalProjectApiService.getProjectName(pid);
                List<ManagerInfoDTO> managers = externalProjectApiService.getManagersForProject(pid);
                return new ProjectManagerInfoDTO(pid, name, managers);
            })
            .toList();

        return ResponseEntity.ok(result);
    }
    


    @GetMapping("/managers")
    public ResponseEntity<List<ManagerUserMappingDTO>> getUsersAssignedToManagers() {

        Map<Long, Set<Long>> managerToUserIdsMap = new HashMap<>();

        // Step 1: Get all timesheets and their entries
        List<TimeSheet> timeSheets = timeSheetRepository.findAll();

        for (TimeSheet timeSheet : timeSheets) {
            Long userId = timeSheet.getUserId();

            // For each entry under the timesheet, get projectId
            for (TimeSheetEntry entry : timeSheet.getEntries()) {
                Long projectId = entry.getProjectId();

                // Step 2: Fetch managers for that project
                List<ManagerInfoDTO> managers = externalProjectApiService.getManagersForProject(projectId);

                // Step 3: Assign user to each manager
                for (ManagerInfoDTO manager : managers) {
                    managerToUserIdsMap
                        .computeIfAbsent(manager.getManagerId(), k -> new HashSet<>())
                        .add(userId);
                }
            }
        }

        // Step 4: Build response DTOs
        List<ManagerUserMappingDTO> result = managerToUserIdsMap.entrySet().stream()
            .map(entry -> {
                Long managerId = entry.getKey();
                Set<Long> userIds = entry.getValue();

                String managerName = externalProjectApiService.getManagersForProject(0L).stream()
                        .filter(m -> m.getManagerId().equals(managerId))
                        .map(ManagerInfoDTO::getManagerName)
                        .findFirst()
                        .orElse("Unknown");

                List<UserSDTO> users = userIds.stream()
                        .map(uid -> new UserSDTO(uid, externalProjectApiService.getUserNameById(uid)))
                        .toList();

                return new ManagerUserMappingDTO(managerId, managerName, users);
            })
            .toList();

        return ResponseEntity.ok(result);
    }
}
