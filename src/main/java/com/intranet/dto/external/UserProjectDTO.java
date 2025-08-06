package com.intranet.dto.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProjectDTO {
    private Long projectId;
    private String projectName;
  
}
