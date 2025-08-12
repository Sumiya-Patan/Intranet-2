package com.intranet.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intranet.entity.TimeSheet;

@Repository
public interface TimeSheetRepo extends JpaRepository<TimeSheet, Long> {

    List<TimeSheet> findByUserIdOrderByWorkDateDesc(Long userId);

    List<TimeSheet> findByUserId(Long userId);
    
    List<TimeSheet> findByUserIdIn(Set<Long> userIds);

}

