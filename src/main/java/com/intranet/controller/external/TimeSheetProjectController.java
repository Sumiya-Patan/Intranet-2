package com.intranet.controller.external;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.intranet.dto.UserDTO;
import com.intranet.dto.external.ManagerUserMappingDTO;
import com.intranet.dto.external.ProjectTaskView;
import com.intranet.security.CurrentUser;
import com.intranet.service.TimeSheetService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/timesheet/project-info")
@RequiredArgsConstructor
public class TimeSheetProjectController {

    @Autowired
    private final TimeSheetService timesheetService;

    @Operation(summary = "Get Project and Task of a  current user")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('GENERAL') or hasRole('HR')")
    @GetMapping
    public ResponseEntity<List<ProjectTaskView>> getTimesheetView(@CurrentUser UserDTO user) {
        List<ProjectTaskView> response = timesheetService.getUserTaskView(user.getId());
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Manager and Project info of a current user")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('GENERAL') or hasRole('HR')")
    @GetMapping("/managers")
    public ResponseEntity<List<ManagerUserMappingDTO>> getUsersAssignedToManagers(
            @CurrentUser UserDTO user) {
        List<ManagerUserMappingDTO> result = timesheetService.getUsersAssignedToManagers(user.getId());
        return ResponseEntity.ok(result);
    }
    
}
