package com.intranet.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "timesheet_on_holiday_type")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheetOnHolidaysType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link with Timesheet
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id", nullable = false,unique = true)
    private TimeSheet timeSheet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HolidayType holidayType;

    public enum HolidayType {
        GENERAL,
        LEAVE
    }

    private LocalDateTime createdAt;
}