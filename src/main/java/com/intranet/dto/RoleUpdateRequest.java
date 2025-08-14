package com.intranet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleUpdateRequest {
    private String fromRole; // e.g. ROLE_GENERAL
    private String toRole;   // e.g. ROLE_USER
}
