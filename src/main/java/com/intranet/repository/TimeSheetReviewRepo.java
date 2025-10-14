package com.intranet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intranet.entity.TimeSheetReview;

@Repository
public interface TimeSheetReviewRepo  extends JpaRepository<TimeSheetReview, Long> {

}
