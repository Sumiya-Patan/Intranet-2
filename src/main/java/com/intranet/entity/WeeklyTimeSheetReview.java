package com.intranet.entity;


import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weekly_timesheet_review")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTimeSheetReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_info_id")
    private WeekInfo weekInfo;


    @Column(nullable = false)
    private Long userId; // link who submitted this week

    @Column(nullable = false)
    private java.time.LocalDateTime submittedAt; // when it was submitted


    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        DRAFT, PENDING, APPROVED, PARTIALLY_REJECTED, REJECTED, SUBMITTED, PARTIALLY_APPROVED
    }

    @Column
    private LocalDateTime reviewedAt; // when it was reviewed
}
