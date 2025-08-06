package com.intranet.controller.external;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.intranet.dto.external.UserProjectDTO;

import java.util.*;

@RestController
@RequestMapping("/api/projects")
public class UserProjectController {

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserProjectDTO>> getUserProjects(@PathVariable Long userId) {

        // Step 1: Mock user-to-project mapping
        Map<Long, List<Long>> userProjectMap = new HashMap<>();
        userProjectMap.put(1L, Arrays.asList(101L, 102L));
        userProjectMap.put(2L, Arrays.asList(103L));
        userProjectMap.put(3L, Arrays.asList(101L, 103L, 104L));

        // Step 2: Mock project details
        Map<Long, String> projectDetails = new HashMap<>();
        projectDetails.put(101L, "Project Alpha");
        projectDetails.put(102L, "Project Beta");
        projectDetails.put(103L, "Project Gamma");
        projectDetails.put(104L, "Project Delta");

        // Step 3: Fetch project IDs for user
        List<Long> projectIds = userProjectMap.getOrDefault(userId, Collections.emptyList());

        // Step 4: Map to DTOs
        List<UserProjectDTO> result = new ArrayList<>();
        for (Long pid : projectIds) {
            String name = projectDetails.get(pid);
            if (name != null) {
                result.add(new UserProjectDTO(pid, name));
            }
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("tasks/user/{userId}")
    public ResponseEntity<List<UserTaskDTO>> getUserTasks(@PathVariable Long userId) {
        
        // Mock: projectId -> projectName (reuse)
        Map<Long, String> projectDetails = new HashMap<>();
        projectDetails.put(101L, "Project Alpha");
        projectDetails.put(102L, "Project Beta");
        projectDetails.put(103L, "Project Gamma");
        projectDetails.put(104L, "Project Delta");

        // Mock: userId -> list of task entries (taskId, taskName, projectId)
        Map<Long, List<Map<String, Object>>> userTasksMap = new HashMap<>();

        userTasksMap.put(1L, Arrays.asList(
                Map.of("taskId", 1001L, "taskName", "Design Login UI", "projectId", 101L),
                Map.of("taskId", 1002L, "taskName", "API Integration", "projectId", 102L)
        ));

        userTasksMap.put(2L, Arrays.asList(
                Map.of("taskId", 1003L, "taskName", "Write Unit Tests", "projectId", 103L)
        ));

        userTasksMap.put(3L, Arrays.asList(
                Map.of("taskId", 1004L, "taskName", "Create Database Schema", "projectId", 104L),
                Map.of("taskId", 1005L, "taskName", "Implement Auth Flow", "projectId", 103L)
        ));

        List<Map<String, Object>> userTasks = userTasksMap.getOrDefault(userId, Collections.emptyList());

        List<UserTaskDTO> result = new ArrayList<>();
        for (Map<String, Object> task : userTasks) {
            Long taskId = (Long) task.get("taskId");
            String taskName = (String) task.get("taskName");
            Long projectId = (Long) task.get("projectId");
            String projectName = projectDetails.getOrDefault(projectId, "Unknown Project");

            result.add(new UserTaskDTO(taskId, taskName, projectId, projectName));
        }

        return ResponseEntity.ok(result);
    }
}
