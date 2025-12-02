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

  public InternalProject createInternalTask(String taskName) {

    if (taskName == null || taskName.trim().isEmpty()) {
        throw new IllegalArgumentException("Task name cannot be empty");
    }

    // Get latest negative task ID
    Integer latestTaskId = repository.findTopByOrderByTaskIdAsc()
            .map(InternalProject::getTaskId)
            .orElse(0);

    // Generate next negative taskId
    int newTaskId = latestTaskId > 0 ? -1 : latestTaskId - 1;

    // Ensure uniqueness - regenerate if duplicate
    while (repository.existsByTaskId(newTaskId)) {
        newTaskId--; // reduce further into negative space
    }

    InternalProject project = new InternalProject();
    project.setProjectId(-1);
    project.setProjectName("Internal Activities");
    project.setTaskId(newTaskId);
    project.setTaskName(taskName);
    project.setBillable(false);

    return repository.save(project);
    }

    // READ ALL
    public List<InternalProject> getAllProjects() {
        return repository.findAll();
    }
    
    public InternalProject updateInternalTask(Long id, String taskName) {

    if (taskName == null || taskName.trim().isEmpty()) {
        throw new IllegalArgumentException("Task name cannot be empty");
    }

    return repository.findById(id)
            .map(existing -> {
                existing.setTaskName(taskName);
                return repository.save(existing);
            })
            .orElseThrow(() ->
                    new RuntimeException("Internal Project not found with id: " + id));
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
