package com.intranet.service;

import com.intranet.entity.InternalProject;
import com.intranet.repository.InternalProjectRepo;
import com.intranet.repository.TimeSheetEntryRepo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalProjectService {

    private final InternalProjectRepo repository;

    private final TimeSheetEntryRepo timeSheetEntryRepo;

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
    @Transactional
        public String deleteProject(Long id) {

            // Step 1: Validate project exists
            InternalProject project = repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Internal Project not found"));

            Integer projectId = project.getProjectId();
            Integer taskId = project.getTaskId();

            // Step 2: Check if this (projectId + taskId) combination is used in timesheets
            boolean existsInTimeSheet = timeSheetEntryRepo
                    .existsByProjectIdAndTaskId(projectId, taskId);

            if (existsInTimeSheet) {
                throw new IllegalArgumentException(
                        String.format(
                                "Cannot delete Internal Project because task is  already logged  in Timesheet entries."));
            }
            // Step 3: Safe delete
            repository.deleteById(id);

            return "Internal Project deleted successfully";
}

}
