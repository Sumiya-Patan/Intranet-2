package com.intranet.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(
    name = "holiday_exclude_users",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "manager_id", "holiday_date"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayExcludeUsers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long managerId;
    private LocalDate holidayDate;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "holidayExcludeUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSheetOnHolidays> holidayTimeSheets = new ArrayList<>();

    
}
