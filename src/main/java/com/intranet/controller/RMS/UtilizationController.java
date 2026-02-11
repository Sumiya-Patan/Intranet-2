package com.intranet.controller.RMS;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.intranet.dto.rms.UserMonthlyUtilizationDTO;
import com.intranet.service.RMS.UtilizationService;

@RestController
@RequestMapping("/api/utilization")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UtilizationController {

    private final UtilizationService utilizationService;

    @GetMapping("/monthly/{userId}")
    public ResponseEntity<UserMonthlyUtilizationDTO> getMonthlyUtilization(
            @PathVariable Long userId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(
                utilizationService.getMonthlyUtilization(userId, year, month)
        );
    }
}
