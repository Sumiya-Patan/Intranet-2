package com.intranet.dto.external;

import java.util.List;

public class ProjectTaskView {
    private String project;
    private List<TaskDTO> tasks;

    public ProjectTaskView(String project, List<TaskDTO> tasks) {
        this.project = project;
        this.tasks = tasks;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public List<TaskDTO> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDTO> tasks) {
        this.tasks = tasks;
    }
}
