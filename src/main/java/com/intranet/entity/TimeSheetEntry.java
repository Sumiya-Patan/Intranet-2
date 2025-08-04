package com.intranet.entity;

import java.math.BigDecimal;
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
@Table(name = "TimeSheetEntry", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"timesheet_id", "task_id", "project_id"})
})
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TimeSheetEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long timesheetEntryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id", nullable = false)
    private TimeSheet timeSheet;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long taskId;

    @Column(length = 255)
    private String description;

    @Column(length = 50, nullable = false)
    private String workType = "Office";

    @Column
    private BigDecimal hoursWorked;

    @Column
    private LocalDateTime fromTime;

    @Column
    private LocalDateTime toTime;

    @Column(length = 255)
    private String otherDescription;

    public Long getUserId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserId'");
    }

    public TimeSheetEntry[] getEntries() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEntries'");
    }
   

}
