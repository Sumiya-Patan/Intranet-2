package com.intranet.controller.external;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.intranet.dto.UserDTO;
import com.intranet.dto.UserSDTO;
import com.intranet.dto.external.ManagerInfoDTO;
import com.intranet.dto.external.ManagerUserMappingDTO;
import com.intranet.dto.external.ProjectManagerInfoDTO;
import com.intranet.dto.external.ProjectTaskView;
import com.intranet.dto.external.ProjectWithUsersDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.repository.TimeSheetEntryRepo;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.security.CurrentUser;
import com.intranet.service.TimeSheetService;
import com.intranet.service.external.ExternalProjectApiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "*", allowedHeaders = "*")
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

    @Autowired
    private final TimeSheetService timesheetService;

    @Operation(summary = "Get Project and Task of a  current user")
    @GetMapping
    public ResponseEntity<List<ProjectTaskView>> getTimesheetView(@CurrentUser UserDTO user) {
        List<ProjectTaskView> response = timesheetService.getUserTaskView(user.getId());
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Manager and Project info of a current user")
    @GetMapping("/managers")
    public ResponseEntity<List<ManagerUserMappingDTO>> getUsersAssignedToManagers(
            @CurrentUser UserDTO user) {
        List<ManagerUserMappingDTO> result = timesheetService.getUsersAssignedToManagers(user.getId());
        return ResponseEntity.ok(result);
    }
    
}
