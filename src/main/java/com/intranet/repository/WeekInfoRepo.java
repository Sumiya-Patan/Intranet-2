package com.intranet.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intranet.entity.WeekInfo;



@Repository
public interface WeekInfoRepo extends JpaRepository<WeekInfo, Long>{

    Optional<WeekInfo> findByStartDateAndEndDate(LocalDate startDate, LocalDate endDate);
    Optional<WeekInfo> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate startDate, LocalDate endDate);
    List<WeekInfo> findByStartDateGreaterThanEqualAndEndDateLessThanEqualOrderByStartDateAsc(LocalDate start, LocalDate end);
    List<WeekInfo> findByStartDateBetween(LocalDate startDate, LocalDate endDate);
}
