package com.intranet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "InternalProject")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalProject {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Primary key

    @Column(nullable = false, length = 100)
    private Integer projectId;  // Internal project name

    @Column(nullable = false, length = 100)
    private String  projectName;  // Internal project name

    @Column(nullable = false, length = 100)
    private Integer taskId;  // Manager of the internal project

    @Column(nullable = false, length = 100)
    private String taskName;  // Manager of the internal project
}
