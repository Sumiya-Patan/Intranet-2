package com.intranet.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "TimeSheet", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "work_date"})
})
public class TimeSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long timesheetId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private String status = "Pending";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "timeSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSheetEntry> entries = new ArrayList<>();

    @OneToMany(mappedBy = "timeSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSheetReview> reviews = new ArrayList<>();
}
