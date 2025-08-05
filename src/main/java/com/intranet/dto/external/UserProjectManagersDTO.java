package com.intranet.dto.external;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProjectManagersDTO {
    private Long userId;
    private List<ProjectManagerInfo> projectManagers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectManagerInfo {
        private Long projectId;
        private Long managerId;
        private String managerName;
    }
}
