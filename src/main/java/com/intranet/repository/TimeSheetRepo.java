package com.intranet.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import com.intranet.entity.TimeSheet;

@Repository
public interface TimeSheetRepo extends JpaRepository<TimeSheet, Long> {
     Optional<TimeSheet> findByUserIdAndWorkDate(Long userId, LocalDate workDate);

     List<TimeSheet> findByUserIdAndWeekInfo_IdInOrderByWorkDateAsc(Long userId, List<Long> weekIds);

     List<TimeSheet> findByWorkDateBetween(LocalDate startDate, LocalDate endDate);

     @Query("SELECT ts FROM TimeSheet ts LEFT JOIN FETCH ts.weekInfo LEFT JOIN FETCH ts.entries WHERE ts.workDate BETWEEN :startDate AND :endDate")
     List<TimeSheet> findByWorkDateBetweenWithWeekInfoAndEntries(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

     @Query("SELECT t FROM TimeSheet t " +
       "JOIN FETCH t.weekInfo w " +
       "WHERE t.userId IN :userIds AND t.status = 'SUBMITTED'")
     List<TimeSheet> findSubmittedByUserIds(@Param("userIds") Set<Long> userIds);

     List<TimeSheet> findByUserIdAndWeekInfo_Id(Long userId, Long weekInfoId);

}
