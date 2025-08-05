package com.intranet.service.external;

import com.intranet.dto.external.ManagerProjectInfoDTO;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProjectManagementService {

    // Simulate an external PMS response
    public List<ManagerProjectInfoDTO> getManagersByProjectIds(List<Long> projectIds) {
        // Mock data (In real case, fetch from external API)
        Map<Long, ManagerProjectInfoDTO> projectToManager = Map.of(
            101L, new ManagerProjectInfoDTO(1L, "Alice", List.of(101L)),
            102L, new ManagerProjectInfoDTO(2L, "Bob", List.of(102L)),
            103L, new ManagerProjectInfoDTO(1L, "Alice", List.of(103L))
        );

        return projectIds.stream()
                .distinct()
                .map(projectToManager::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

     public Map<Long, ManagerProjectInfoDTO> getProjectManagerMap() {
        Map<Long, ManagerProjectInfoDTO> map = new HashMap<>();

        map.put(0L, new ManagerProjectInfoDTO(100L, "Manager A", List.of(0L)));
        map.put(1L, new ManagerProjectInfoDTO(101L, "Manager B", List.of(1L)));
        map.put(3L, new ManagerProjectInfoDTO(102L, "Manager C", List.of(3L)));
        map.put(32L, new ManagerProjectInfoDTO(103L, "Manager D", List.of(32L)));

        return map;
    }

}
