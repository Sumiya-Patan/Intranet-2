package com.intranet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EndpointRoleRequest {
    private Long id;        // Optional, for delete operations
    private String path;     // e.g. "/users"
    private String method;   // e.g. "POST"
    private String roleName; // e.g. "ROLE_MANAGER"
}
