package com.intranet.controller.external;

import com.intranet.dto.UserDTO;
import com.intranet.dto.external.UserProjectManagersDTO;
import com.intranet.security.CurrentUser;
import com.intranet.service.external.TimeSheetManagerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timesheets")
public class TimeSheetUserManagerController {

    @Autowired
    private TimeSheetManagerService managerService;

    @GetMapping("/managers")
    public ResponseEntity<UserProjectManagersDTO> getUserProjectManagers(@CurrentUser UserDTO user) {
        UserProjectManagersDTO dto = managerService.getManagersByUserId(user.getId());
        return ResponseEntity.ok(dto);
    }
}
