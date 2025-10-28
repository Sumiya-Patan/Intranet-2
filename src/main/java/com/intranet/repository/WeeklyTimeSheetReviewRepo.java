package com.intranet.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.intranet.entity.WeeklyTimeSheetReview;

@Repository
public interface WeeklyTimeSheetReviewRepo extends JpaRepository<WeeklyTimeSheetReview, Long>{

    Optional<WeeklyTimeSheetReview> findByUserIdAndWeekInfo_Id(Long userId, Long weekInfoId);

    List<WeeklyTimeSheetReview> findByUserIdAndWeekInfo_StartDateBetween(Long userId, LocalDate startDate,
            LocalDate endDate);
}
