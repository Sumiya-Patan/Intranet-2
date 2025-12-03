package com.intranet.dto.pms;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUserRequestDTO {
    private List<Long> userIds;
    private Long projectId;
}
