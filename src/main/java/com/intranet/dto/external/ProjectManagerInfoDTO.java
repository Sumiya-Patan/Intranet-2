package com.intranet.dto.external;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectManagerInfoDTO {
    private Long projectId;
    private String projectName;
    private List<ManagerInfoDTO> managers;
}

