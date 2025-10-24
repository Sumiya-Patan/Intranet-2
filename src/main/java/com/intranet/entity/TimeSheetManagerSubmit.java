// TimeSheetManagerSubmit.java
package com.intranet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "timesheet_manager_submit",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "timesheet_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheetManagerSubmit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id")
    private TimeSheet timeSheet;

    private Boolean submittedByManager;
    private Long managerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    
}
