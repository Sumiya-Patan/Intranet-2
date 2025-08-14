package com.intranet.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.intranet.entity.EndpointRole;
import com.intranet.repository.EndpointRoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EndpointRoleService {

    private final EndpointRoleRepository endpointRoleRepository;

    public boolean hasAccess(String path, String method, Authentication authentication) {
        // Normalize the path
        String normalizedPath = path.startsWith("/") ? path : "/" + path;

        // Get all required roles for this path+method from DB
        List<String> requiredRoles = endpointRoleRepository
                .findByPathAndMethod(normalizedPath, method.toUpperCase())
                .stream()
                .map(EndpointRole::getRoleName)
                .toList();

        if (requiredRoles.isEmpty()) {
            return true; // No restriction in DB = allow
        }

        // Check if the user has at least one required role
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(requiredRoles::contains);
    }
}
