package com.intranet.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "timesheet_review")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheetReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_info_id")
    private WeekInfo weekInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id")
    private TimeSheet timeSheet; // Optional - for daily reviews

    private Long managerId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String comments;
    private LocalDateTime reviewedAt;

    public enum Status {
        APPROVED, REJECTED, PENDING, SUBMITTED
    }

}
