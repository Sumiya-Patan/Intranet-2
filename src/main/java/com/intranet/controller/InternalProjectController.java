package com.intranet.controller;

import com.intranet.entity.InternalProject;
import com.intranet.service.InternalProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal-projects")
@RequiredArgsConstructor
public class InternalProjectController {

    private final InternalProjectService service;

    // CREATE
    @PostMapping
    public ResponseEntity<InternalProject> create(@RequestBody InternalProject project) {
        return ResponseEntity.ok(service.createProject(project));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<InternalProject>> getAll() {
        return ResponseEntity.ok(service.getAllProjects());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<InternalProject> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getProjectById(id));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<InternalProject> update(
            @PathVariable Long id,
            @RequestBody InternalProject project
    ) {
        return ResponseEntity.ok(service.updateProject(id, project));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.deleteProject(id));
    }
}
