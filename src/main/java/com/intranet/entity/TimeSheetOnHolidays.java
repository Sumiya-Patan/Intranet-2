// TimeSheetOnHolidays.java
package com.intranet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "timesheet_on_holidays")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheetOnHolidays {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id", unique = true)
    private TimeSheet timeSheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holi_ex_user_id")
    private HolidayExcludeUsers holidayExcludeUser;

    private Boolean isHoliday;
    private LocalDateTime holidayDate;
    private String description;

    
}
