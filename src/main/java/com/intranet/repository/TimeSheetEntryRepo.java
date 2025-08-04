package com.intranet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intranet.entity.TimeSheetEntry;

@Repository
public interface TimeSheetEntryRepo extends JpaRepository<TimeSheetEntry, Long> {}