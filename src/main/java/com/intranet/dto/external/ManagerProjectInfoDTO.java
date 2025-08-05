package com.intranet.dto.external;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManagerProjectInfoDTO {
    private Long managerId;
    private String managerName;
    private List<Long> projectIds;
}