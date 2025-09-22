// package com.intranet.entity;

// import jakarta.persistence.Id;
// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import lombok.AllArgsConstructor;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// @Entity
// @NoArgsConstructor
// @AllArgsConstructor
// @Data
// public class EndpointRole {
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     @Column(nullable = false)
//     private String path; // e.g. "/users"

//     @Column(nullable = false)
//     private String method; // e.g. "GET", "POST"

//     @Column(nullable = false)
//     private String roleName; // e.g. "ROLE_ADMIN"

//     @Column(nullable = false)
//     private String permission;
// }