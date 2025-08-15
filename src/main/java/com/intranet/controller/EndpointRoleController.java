package com.intranet.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import com.intranet.dto.EndpointRoleRequest;
import com.intranet.dto.RoleUpdateRequest;
import com.intranet.entity.EndpointRole;
import com.intranet.repository.EndpointRoleRepository;

import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/endpoint-roles")
@RequiredArgsConstructor
public class EndpointRoleController {

    private final EndpointRoleRepository endpointRoleRepository;

    @Operation(summary = "Assign a role to an endpoint")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<String> assignRole(@RequestBody EndpointRoleRequest request) {
        // Normalize path (remove IP/port, just ensure it starts with "/")
        String normalizedPath = request.getPath().startsWith("/") ? request.getPath() : "/" + request.getPath();

        // Check if already exists to avoid duplicates
        boolean exists = endpointRoleRepository.existsByPathAndMethodAndRoleName(
                normalizedPath,
                request.getMethod().toUpperCase(),
                request.getRoleName().toUpperCase()
        );

        if (exists) {
            return ResponseEntity.badRequest().body("Role already assigned to this endpoint");
        }

        EndpointRole role = new EndpointRole();
        role.setPath(normalizedPath);
        role.setMethod(request.getMethod().toUpperCase());
        role.setRoleName(request.getRoleName().toUpperCase());

        endpointRoleRepository.save(role);

        return ResponseEntity.ok("Role assigned to endpoint successfully");
    }


        
    @Operation(summary = "Get all roles assigned to endpoints")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<EndpointRole>> getAllRoles() {
        return ResponseEntity.ok(endpointRoleRepository.findAll());
    }


    @Operation(summary = "Delete a role mapping from an endpoint by ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    // @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteRoleFromEndpoint(@PathVariable Long id) {
        // String normalizedPath = request.getPath().startsWith("/") ? request.getPath() : "/" + request.getPath();

        // endpointRoleRepository.deleteByPathAndMethodandRoleName(
        //         normalizedPath,
        //         request.getMethod().toUpperCase(),
        //         request.getRoleName().toUpperCase()
        // );

        endpointRoleRepository.deleteById(id);

        return ResponseEntity.ok("Role mapping deleted successfully");
    }


    private final RequestMappingHandlerMapping handlerMapping;
    // private final EndpointRoleRepository endpointRoleRepository;
    @Operation(summary = "Get all endpoints with their roles of the application")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @GetMapping("/endpoints-of-app")
    public List<EndpointInfo> getAllEndpoints() {
    List<EndpointInfo> endpoints = new ArrayList<>();

    handlerMapping.getHandlerMethods().forEach((mapping, handlerMethod) -> {
        mapping.getMethodsCondition().getMethods().forEach(requestMethod -> {

            // Handle Spring Boot 3+ path pattern style
            Set<String> patterns;
            if (mapping.getPathPatternsCondition() != null) {
                patterns = mapping.getPathPatternsCondition().getPatterns()
                        .stream()
                        .map(PathPattern::getPatternString)
                        .collect(Collectors.toSet());
            }
            // Fallback for older Spring Boot versions
            else if (mapping.getPatternsCondition() != null) {
                patterns = mapping.getPatternsCondition().getPatterns();
            } else {
                patterns = Collections.emptySet();
            }

            for (String urlPattern : patterns) {
                // Normalize path
                String path = urlPattern.startsWith("/") ? urlPattern : "/" + urlPattern;

                // Fetch roles from DB
                List<String> roles = endpointRoleRepository
                        .findByPathAndMethod(path, requestMethod.name())
                        .stream()
                        .map(EndpointRole::getRoleName)
                        .toList();

                endpoints.add(new EndpointInfo(requestMethod.name(), path, roles));
            }
        });
    });

    return endpoints;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public static class EndpointInfo {
    private String method;
    private String path;
    private List<String> roleName;
    }


    @Operation(summary = "Update role name for all endpoints")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PutMapping("/update-role")
    public ResponseEntity<String> updateRoleName(@RequestBody RoleUpdateRequest request) {
        // Fetch all records with the old role name
        List<EndpointRole> roles = endpointRoleRepository.findByRoleName(request.getFromRole().toUpperCase());

        if (roles.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No records found for role: " + request.getFromRole());
        }

        // Update to new role name
        roles.forEach(role -> role.setRoleName(request.getToRole().toUpperCase()));
        endpointRoleRepository.saveAll(roles);

        return ResponseEntity.ok("Updated role '" + request.getFromRole() +
                "' to '" + request.getToRole() + "' successfully for " + roles.size() + " records.");
    }
}
