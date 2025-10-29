package com.intranet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intranet.entity.TimeSheetOnHolidays;



@Repository
public interface TimeSheetOnHolidaysRepo extends JpaRepository<TimeSheetOnHolidays, Long>{

    boolean existsByTimeSheetId(Long id);
    Optional<TimeSheetOnHolidays> findByTimeSheetId(Long timeSheetId);


}
