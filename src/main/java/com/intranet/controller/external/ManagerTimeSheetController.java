package com.intranet.controller.external;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.TimeSheetEntryResponseDTO;
import com.intranet.dto.TimeSheetResponseDTO;
import com.intranet.dto.external.ManagerInfoDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.service.external.ExternalProjectApiService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class ManagerTimeSheetController {

    private final TimeSheetRepo timeSheetRepository;
    private final ExternalProjectApiService externalProjectApiService;

    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<TimeSheetResponseDTO>> getTimesheetsByManager(@PathVariable Long managerId) {

        // Step 1: Fetch all timesheets with entries
        List<TimeSheet> allTimeSheets = timeSheetRepository.findAll();

        Set<Long> userIdsForManager = new HashSet<>();

        // Step 2: Loop through all entries to find projects assigned to manager
        for (TimeSheet sheet : allTimeSheets) {
            Long userId = sheet.getUserId();
            for (TimeSheetEntry entry : sheet.getEntries()) {
                List<ManagerInfoDTO> managers = externalProjectApiService.getManagersForProject(entry.getProjectId());
                boolean managerMatches = managers.stream().anyMatch(m -> m.getManagerId().equals(managerId));

                if (managerMatches) {
                    userIdsForManager.add(userId);
                }
            }
        }

        // Step 3: Filter timesheets by those user IDs
        List<TimeSheetResponseDTO> result = allTimeSheets.stream()
                .filter(ts -> userIdsForManager.contains(ts.getUserId()))
                .map(ts -> {
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
                    // tsDto.setCreatedAt(ts.getCreatedAt());
                    // tsDto.setUpdatedAt(ts.getUpdatedAt());
                    tsDto.setEntries(entries);
                    return tsDto;
                })
                .toList();

        return ResponseEntity.ok(result);
    }
}
