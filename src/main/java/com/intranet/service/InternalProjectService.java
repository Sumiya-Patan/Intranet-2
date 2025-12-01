package com.intranet.service;

import com.intranet.entity.InternalProject;
import com.intranet.repository.InternalProjectRepo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalProjectService {

    private final InternalProjectRepo repository;

    // CREATE
    public InternalProject createProject(InternalProject project) {
        return repository.save(project);
    }

    // READ ALL
    public List<InternalProject> getAllProjects() {
        return repository.findAll();
    }

    // READ BY ID
    public InternalProject getProjectById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Internal Project not found with id: " + id));
    }

    // UPDATE
    public InternalProject updateProject(Long id, InternalProject updatedProject) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setProjectId(updatedProject.getProjectId());
                    existing.setProjectName(updatedProject.getProjectName());
                    existing.setTaskId(updatedProject.getTaskId());
                    existing.setTaskName(updatedProject.getTaskName());
                    existing.setBillable(updatedProject.isBillable());
                    return repository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Internal Project not found with id: " + id));
    }

    // DELETE
    public String deleteProject(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Internal Project not found with id: " + id);
        }
        repository.deleteById(id);
        return "Internal Project deleted successfully";
    }
}
