package com.intranet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.intranet.entity.WeeklyTimeSheetReview;

@Repository
public interface WeeklyTimeSheetReviewRepo extends JpaRepository<WeeklyTimeSheetReview, Long>{

    Optional<WeeklyTimeSheetReview> findByUserIdAndWeekInfo_Id(Long userId, Long weekInfoId);
}
