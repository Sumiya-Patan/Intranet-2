package com.intranet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "timesheet_entry")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheetEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id")
    private TimeSheet timeSheet;

    private Long projectId;
    private Long taskId;
    private String description;
    private LocalTime fromTime;
    private LocalTime toTime;
    private BigDecimal hoursWorked;
    private String otherDescription;
}
