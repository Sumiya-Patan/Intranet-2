package com.intranet.controller;

import com.intranet.service.InternalProjectService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/internal-projects")
@RequiredArgsConstructor
public class InternalProjectController {

    private final InternalProjectService service;

    // CREATE
    @PostMapping("/create")
    @Operation(summary = "Create Internal Project")
    @PreAuthorize("hasAuthority('TIMESHEET_ADMIN')")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        String taskName = body.get("taskName");
        try{
            service.createInternalTask(taskName);
            return ResponseEntity.ok("Internal Project created successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // READ ALL
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('TIMESHEET_ADMIN')")
    @Operation(summary = "Get all Internal Projects")
    public ResponseEntity<?> getAll() {
        try{
            return ResponseEntity.ok(service.getAllProjects());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch Internal Projects");
        }
    }

    // UPDATE
    @PutMapping("/{id}")
    @Operation(summary = "Update Internal Project")
    @PreAuthorize("hasAuthority('TIMESHEET_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody
    ) {
        String taskName = requestBody.get("taskName");

        try{
            service.updateInternalTask(id, taskName);
            return ResponseEntity.ok("Internal Project updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update Internal Project");
        }
    }


    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.deleteProject(id));
    }
}
