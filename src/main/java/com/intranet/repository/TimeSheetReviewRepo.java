package com.intranet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intranet.entity.TimeSheetReview;

@Repository
public interface TimeSheetReviewRepo  extends JpaRepository<TimeSheetReview, Long> {

    Optional<TimeSheetReview> findByTimeSheet_IdAndManagerId(Long timesheetId, Long managerId);

    Optional<TimeSheetReview> findByWeekInfo_IdAndUserIdAndManagerId(Long weekInfoId, Long userId, Long managerId);

}
