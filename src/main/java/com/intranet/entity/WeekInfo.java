package com.intranet.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(
    name = "week_info",
    uniqueConstraints = @UniqueConstraint(columnNames = {"start_date", "end_date"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeekInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer weekNo;
    private Integer year;
    private Integer month;
    private Boolean incompleteWeek;

    @OneToMany(mappedBy = "weekInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSheet> timeSheets = new ArrayList<>();

    @OneToMany(mappedBy = "weekInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSheetReview> timeSheetReviews = new ArrayList<>();

    @OneToMany(mappedBy = "weekInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeeklyTimeSheetReview> weeklyReviews = new ArrayList<>();

}
