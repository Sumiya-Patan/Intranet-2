package com.intranet.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.intranet.dto.TimeSheetEntryDTO;
import com.intranet.dto.TimeSheetEntryResponseDTO;
import com.intranet.dto.TimeSheetEntryUpdateDTO;
import com.intranet.dto.TimeSheetResponseDTO;
import com.intranet.dto.TimeSheetUpdateRequestDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.entity.TimeSheetReview;
import com.intranet.repository.TimeSheetEntryRepo;
import com.intranet.repository.TimeSheetRepo;

import jakarta.transaction.Transactional;

@Service
public class TimeSheetService {

    @Autowired
    private TimeSheetRepo timeSheetRepository;

    @Autowired
    private TimeSheetEntryRepo timeSheetEntryRepository;
    
  public void createTimeSheetWithEntries(
        Long userId,
        LocalDate workDate,
        List<TimeSheetEntryDTO> entriesDto) {

    // 🔹 Step 1: Calculate hours if from/to time is provided
    for (TimeSheetEntryDTO dto : entriesDto) {
        if (dto.getFromTime() != null && dto.getToTime() != null) {
            long minutes = Duration.between(dto.getFromTime(), dto.getToTime()).toMinutes();
            dto.setHoursWorked(BigDecimal.valueOf(minutes / 60.0));
        } else if (dto.getHoursWorked() == null) {
            dto.setHoursWorked(BigDecimal.ZERO);
        }
    }

    // 🔹 Step 2: Create TimeSheet
    TimeSheet timesheet = new TimeSheet();
    timesheet.setUserId(userId);
    timesheet.setWorkDate(workDate);

    // 🔹 Step 3: Create and attach entries
    List<TimeSheetEntry> entries = new ArrayList<>();
    for (TimeSheetEntryDTO dto : entriesDto) {
        TimeSheetEntry entry = new TimeSheetEntry();
        entry.setTimeSheet(timesheet);
        entry.setProjectId(dto.getProjectId());
        entry.setTaskId(dto.getTaskId());
        entry.setDescription(dto.getDescription());
        entry.setWorkType(dto.getWorkType());
        entry.setFromTime(dto.getFromTime());
        entry.setToTime(dto.getToTime());
        entry.setHoursWorked(dto.getHoursWorked());
        entry.setOtherDescription(dto.getOtherDescription());

        entries.add(entry);
    }
    timesheet.setEntries(entries);

    timeSheetRepository.save(timesheet);
    }

    public List<TimeSheetResponseDTO> getUserTimeSheetHistory(Long userId) {
    List<TimeSheet> timesheets = timeSheetRepository.findByUserIdOrderByWorkDateDesc(userId);

    return timesheets.stream().map(ts -> {
        TimeSheetResponseDTO dto = new TimeSheetResponseDTO();
        dto.setTimesheetId(ts.getTimesheetId());
        dto.setWorkDate(ts.getWorkDate());
        dto.setCreatedAt(ts.getCreatedAt());
        dto.setStatus(ts.getStatus());

        // Map entries
        List<TimeSheetEntryResponseDTO> entryDTOs = ts.getEntries().stream().map(entry -> {
            TimeSheetEntryResponseDTO entryDto = new TimeSheetEntryResponseDTO();
            entryDto.setTimesheetEntryId(entry.getTimesheetEntryId());
            entryDto.setProjectId(entry.getProjectId());
            entryDto.setTaskId(entry.getTaskId());
            entryDto.setDescription(entry.getDescription());
            entryDto.setWorkType(entry.getWorkType());
            entryDto.setFromTime(entry.getFromTime());
            entryDto.setToTime(entry.getToTime());  // Make sure it's getEndTime()
            entryDto.setHoursWorked(entry.getHoursWorked());
            entryDto.setOtherDescription(entry.getOtherDescription());
            return entryDto;
        }).toList();

        dto.setEntries(entryDTOs);
        return dto;
    }).toList();
    }


    @Transactional
    public void partialUpdateTimesheet(Long timesheetId, TimeSheetUpdateRequestDTO dto) {
    TimeSheet timesheet = timeSheetRepository.findById(timesheetId)
            .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

    // 🗓 Update workDate if provided
    if (dto.getWorkDate() != null) {
        timesheet.setWorkDate(dto.getWorkDate());
    }

    // 🔄 Update status if provided
    if (dto.getStatus() != null) {
        timesheet.setStatus(dto.getStatus());
    }

    // 🔄 Update entries
    if (dto.getEntries() != null && !dto.getEntries().isEmpty()) {
        for (TimeSheetEntryUpdateDTO entryDto : dto.getEntries()) {
            TimeSheetEntry entry = timeSheetEntryRepository.findById(entryDto.getTimesheetEntryId())
                    .orElseThrow(() -> new IllegalArgumentException("Entry not found with ID: " + entryDto.getTimesheetEntryId()));

            if (entryDto.getProjectId() != null) entry.setProjectId(entryDto.getProjectId());
            if (entryDto.getTaskId() != null) entry.setTaskId(entryDto.getTaskId());
            if (entryDto.getDescription() != null) entry.setDescription(entryDto.getDescription());
            if (entryDto.getWorkType() != null) entry.setWorkType(entryDto.getWorkType());
            if (entryDto.getFromTime() != null) entry.setFromTime(entryDto.getFromTime());
            if (entryDto.getToTime() != null) entry.setToTime(entryDto.getToTime());
            if (entryDto.getOtherDescription() != null) entry.setOtherDescription(entryDto.getOtherDescription());

            // 🕒 Calculate hours if both from/to time provided
            if (entryDto.getFromTime() != null && entryDto.getToTime() != null) {
                long minutes = Duration.between(entryDto.getFromTime(), entryDto.getToTime()).toMinutes();
                entry.setHoursWorked(BigDecimal.valueOf(minutes / 60.0));
            } else if (entryDto.getHoursWorked() != null) {
                entry.setHoursWorked(entryDto.getHoursWorked());
            }

            timeSheetEntryRepository.save(entry);
        }
    }
    timesheet.setUpdatedAt(LocalDateTime.now());
    timeSheetRepository.save(timesheet);
}

}
