package com.intranet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.intranet.entity.WeeklyTimeSheetReview;

@Repository
public interface WeeklyTimeSheetReviewRepo extends JpaRepository<WeeklyTimeSheetReview, Long>{

}
