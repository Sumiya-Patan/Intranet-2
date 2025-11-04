package com.intranet.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.intranet.entity.WeeklyTimeSheetReview;
import com.intranet.entity.WeeklyTimeSheetReview.Status;

@Repository
public interface WeeklyTimeSheetReviewRepo extends JpaRepository<WeeklyTimeSheetReview, Long>{

    Optional<WeeklyTimeSheetReview> findByUserIdAndWeekInfo_Id(Long userId, Long weekInfoId);

    List<WeeklyTimeSheetReview> findByUserIdAndWeekInfo_StartDateBetween(Long userId, LocalDate startDate,
            LocalDate endDate);
    
    boolean existsByUserIdAndWeekInfoIdAndStatus(Long userId, Long weekInfoId, Status status);

    List<WeeklyTimeSheetReview> findByUserIdInAndWeekInfo_StartDateBetween(
            List<Long> userIds, LocalDate startDate, LocalDate endDate);

    List<WeeklyTimeSheetReview> findByUserIdIn(List<Long> userIds);

    List<WeeklyTimeSheetReview> findByUserIdInAndWeekInfo_StartDateBetween(Set<Long> memberIds, LocalDate startDate,
            LocalDate endDate);
}
