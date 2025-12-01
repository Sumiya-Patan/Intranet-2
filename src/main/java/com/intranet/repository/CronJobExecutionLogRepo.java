package com.intranet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intranet.entity.CronJobExecutionLog;

public interface CronJobExecutionLogRepo extends JpaRepository<CronJobExecutionLog, Long> {}
