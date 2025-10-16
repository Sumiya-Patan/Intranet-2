package com.intranet.dto.external;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskDTO {
    private Long taskId;
    private String task;
    private String description;
    private String startTime;
    private String endTime;

    
}