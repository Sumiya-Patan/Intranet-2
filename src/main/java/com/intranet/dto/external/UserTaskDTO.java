package com.intranet.dto.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserTaskDTO  {
 
    private Long taskId;
    private String taskName;
    private Long projectId;
    private String projectName;
}
