package com.intranet.dto.external;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectTaskView {
    private Long projectId;
    private String project;
    private List<TaskDTO> tasks;
}