package com.intranet.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "email_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String reason;
}
