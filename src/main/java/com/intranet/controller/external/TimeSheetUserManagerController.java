package com.intranet.controller.external;

import com.intranet.dto.external.UserProjectManagersDTO;
import com.intranet.service.external.TimeSheetManagerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timesheets")
public class TimeSheetUserManagerController {

    @Autowired
    private TimeSheetManagerService managerService;

    @GetMapping("/managers/{userId}")
    public ResponseEntity<UserProjectManagersDTO> getUserProjectManagers(@PathVariable Long userId) {
        UserProjectManagersDTO dto = managerService.getManagersByUserId(userId);
        return ResponseEntity.ok(dto);
    }
}
