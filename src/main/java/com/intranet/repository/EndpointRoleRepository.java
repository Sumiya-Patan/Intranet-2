// package com.intranet.repository;

// import java.util.List;

// import org.springframework.data.jpa.repository.JpaRepository;
// import com.intranet.entity.EndpointRole;

// public interface EndpointRoleRepository extends JpaRepository<EndpointRole, Long> {
//     boolean existsByPathAndMethodAndRoleName(String path, String method, String roleName);

//     List<EndpointRole> findByPathAndMethod(String path, String method);

//     // void deleteByPathAndMethod(String normalizedPath, String upperCase);

//     // void deleteByPathAndMethodandRoleName(String normalizedPath, String upperCase, String roleName);

//     List<EndpointRole> findByRoleName(String roleName);
// }
