package com.intranet.service.external;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.intranet.dto.external.ManagerProjectInfoDTO;
import com.intranet.dto.external.UserProjectManagersDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.repository.TimeSheetRepo;

@Service
public class TimeSheetManagerService {

    @Autowired
    private TimeSheetRepo timeSheetRepository;

    @Autowired
    private ProjectManagementService projectManagementService;

    public UserProjectManagersDTO getManagersByUserId(Long userId) {
        // Step 1: Get all timesheets for user
        List<TimeSheet> userSheets = timeSheetRepository.findByUserId(userId);

        // Step 2: Extract unique projectIds from entries
        Set<Long> projectIds = userSheets.stream()
                .filter(sheet -> sheet.getEntries() != null)
                .flatMap(sheet -> sheet.getEntries().stream())
                .map(TimeSheetEntry::getProjectId)
                .collect(Collectors.toSet());

        // Step 3: Get project-to-manager map from external service
        Map<Long, ManagerProjectInfoDTO> projectManagerMap = projectManagementService.getProjectManagerMap();

        // Step 4: Build project-manager info list
        List<UserProjectManagersDTO.ProjectManagerInfo> projectManagerList = projectIds.stream()
                .map(projectId -> {
                    ManagerProjectInfoDTO mgr = projectManagerMap.get(projectId);
                    if (mgr != null) {
                        return new UserProjectManagersDTO.ProjectManagerInfo(
                                projectId,
                                mgr.getManagerId(),
                                mgr.getManagerName()
                        );
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        // Step 5: Return final DTO
        return new UserProjectManagersDTO(userId, projectManagerList);
    }
}

