package com.intranet.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intranet.entity.WeekInfo;



@Repository
public interface WeekInfoRepo extends JpaRepository<WeekInfo, Long>{

    Optional<WeekInfo> findByStartDateAndEndDate(LocalDate startDate, LocalDate endDate);
    Optional<WeekInfo> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate startDate, LocalDate endDate);

}
