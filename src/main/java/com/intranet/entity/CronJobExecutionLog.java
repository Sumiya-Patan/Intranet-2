package com.intranet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cron_job_execution_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CronJobExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobName;  // e.g., "AutoGenerateTimesheetJob"

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;   // SUCCESS, FAILED, RUNNING

    @Column(columnDefinition = "TEXT")
    private String message;  // Error stacktrace or info message
     
    public enum Status {
        RUNNING,
        SUCCESS,
        FAILED
    }
}
