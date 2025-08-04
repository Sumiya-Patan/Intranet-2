package com.intranet.dto.external;

import java.util.List;

import com.intranet.dto.UserSDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManagerUserMappingDTO {
    private Long managerId;
    private String managerName;
    private List<UserSDTO> users;
}
