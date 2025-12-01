package com.intranet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intranet.entity.EmailSettings;

@Repository
public interface EmailSettingsRepo extends JpaRepository<EmailSettings, Long> {
}
