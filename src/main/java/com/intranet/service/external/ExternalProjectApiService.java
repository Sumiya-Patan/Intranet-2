package com.intranet.service.external;

import java.util.List;

import org.springframework.stereotype.Service;

import com.intranet.dto.external.ManagerInfoDTO;

@Service
public class ExternalProjectApiService {

    public String getProjectName(Long projectId) {
        return "Project-" + projectId; // Simulated name
    }

    public List<ManagerInfoDTO> getManagersForProject(Long projectId) {
        // Simulated response
        if (projectId % 2 == 0) {
            return List.of(
                new ManagerInfoDTO(1L, "Ajay Kumar"),
                new ManagerInfoDTO(2L, "Nina Verma")
            );
        } else {
            return List.of(
                new ManagerInfoDTO(3L, "Rahul Mehta")
            );
        }
    }

    
    public String getUserNameById(Long userId) {
        return "User-" + userId;
    }

    // public List<ManagerInfoDTO> getManagersForProject(Long projectId) {
    //     // Mock response. Replace with actual REST call if needed.
    //     return List.of(
    //             new ManagerInfoDTO(1L, "Manager A"),
    //             new ManagerInfoDTO(2L, "Manager B")
    //     );
    // }
}
