package com.intranet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intranet.entity.TimeSheetManagerSubmit;



@Repository
public interface TimeSheetManagerSubmitRepo extends JpaRepository<TimeSheetManagerSubmit, Long>{

}
