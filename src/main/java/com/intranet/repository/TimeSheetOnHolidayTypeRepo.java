package com.intranet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.intranet.entity.TimeSheetOnHolidaysType;

public interface TimeSheetOnHolidayTypeRepo extends JpaRepository<TimeSheetOnHolidaysType, Long> {

    Optional<TimeSheetOnHolidaysType> findByTimeSheetId(Long timeSheetId);
    
    boolean existsByTimeSheetId(Long timeSheetId);
}