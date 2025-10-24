package com.intranet.dto.external;



// import com.intranet.dto.UserSDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectWithUsersDTO {
    private Long projectId;
    private String projectName;
    // private List<UserSDTO> users;
}