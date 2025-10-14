package com.intranet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intranet.entity.WeekInfo;



@Repository
public interface WeekInfoRepo extends JpaRepository<WeekInfo, Long>{

}
