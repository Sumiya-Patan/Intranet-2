package com.intranet.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intranet.entity.HolidayExcludeUsers;

@Repository
public interface HolidayExcludeUsersRepo extends JpaRepository<HolidayExcludeUsers, Long>{

    boolean existsByUserIdAndManagerIdAndHolidayDate(Long userId, Long managerId, LocalDate holidayDate);

    List<HolidayExcludeUsers> findByUserId(Long userId);

    boolean existsByUserIdAndHolidayDate(Long id, LocalDate workDate);

    Optional<HolidayExcludeUsers> findByUserIdAndHolidayDate(Long userId, LocalDate workDate);
}

