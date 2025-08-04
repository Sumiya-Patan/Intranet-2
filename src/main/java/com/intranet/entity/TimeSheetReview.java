package com.intranet.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TimeSheetReview", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"timesheet_id", "manager_id"})
})
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TimeSheetReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id", nullable = false)
    private TimeSheet timeSheet;

    @Column(nullable = false)
    private Long managerId;

    @Column(nullable = false)
    private String action;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime reviewedAt = LocalDateTime.now();
    
}
