package com.intranet.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intranet.entity.InternalProject;

@Repository
public interface InternalProjectRepo  extends  JpaRepository<InternalProject, Long> {
    List<InternalProject> findAll();
}
