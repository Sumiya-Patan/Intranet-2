package com.intranet.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.intranet.entity.TimeSheet;

@Repository
public interface TimeSheetRepo extends JpaRepository<TimeSheet, Long> {
     Optional<TimeSheet> findByUserIdAndWorkDate(Long userId, LocalDate workDate);

     List<TimeSheet> findByUserIdAndWeekInfo_IdInOrderByWorkDateAsc(Long userId, List<Long> weekIds);

}
