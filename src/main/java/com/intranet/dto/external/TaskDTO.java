package com.intranet.dto.external;

public class TaskDTO {
    private String task;
    private String description;
    private String startTime;
    private String endTime;

    public TaskDTO(String task, String description, String startTime, String endTime) {
        this.task = task;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
