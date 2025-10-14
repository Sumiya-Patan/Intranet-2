package com.intranet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(
    name = "timesheet",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "work_date"})
)
@Data
@NoArgsConstructor  
@AllArgsConstructor
public class TimeSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private LocalDate workDate;

    private BigDecimal hoursWorked;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_info_id")
    private WeekInfo weekInfo;

    @OneToMany(mappedBy = "timeSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSheetEntry> entries = new ArrayList<>();

    @OneToOne(mappedBy = "timeSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private TimeSheetOnHolidays timeSheetOnHolidays;

    @OneToMany(mappedBy = "timeSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSheetManagerSubmit> managerSubmissions = new ArrayList<>();

    @OneToMany(mappedBy = "timeSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSheetReview> reviews = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Status {
        DRAFT, SUBMITTED, APPROVED, REJECTED, RE_SUBMITTED
    }

}
