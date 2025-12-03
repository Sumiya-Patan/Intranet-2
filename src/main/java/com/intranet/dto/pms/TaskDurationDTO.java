package com.intranet.dto.pms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskDurationDTO {
    private Long taskId;
    private String duration;  // Format HH:mm (e.g., "7:10")
}
