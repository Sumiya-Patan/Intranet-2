package com.intranet.service;

import com.intranet.dto.HolidayExcludeUsersRequestDTO;
import com.intranet.entity.HolidayExcludeUsers;
import com.intranet.repository.HolidayExcludeUsersRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HolidayExcludeUsersService {

    @Autowired
    private HolidayExcludeUsersRepo repository;

    public String createHolidayExclude(Long managerId,HolidayExcludeUsersRequestDTO request) {

        // Optionally: you can check if record already exists with same user, manager, and date
        if (repository.existsByUserIdAndManagerIdAndHolidayDate(request.getUserId(), managerId, request.getHolidayDate())) {
            throw new IllegalArgumentException("Holiday exclusion already exists for this user on the specified date");
        }
        HolidayExcludeUsers entity = new HolidayExcludeUsers();
        entity.setUserId(request.getUserId());
        entity.setManagerId(managerId);
        entity.setHolidayDate(request.getHolidayDate());
        entity.setReason(request.getReason());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
        return "Holiday exclusion created successfully";
    }
}

