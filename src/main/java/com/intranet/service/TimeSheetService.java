package com.intranet.service;

import com.intranet.dto.AddEntryDTO;
import com.intranet.dto.TimeSheetEntryCreateDTO;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.TimeSheetEntry;
import com.intranet.entity.WeekInfo;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.WeekInfoRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeSheetService {

    private final TimeSheetRepo timeSheetRepository;
    private final WeekInfoRepo weekInfoRepository;

    @Transactional
    public String createTimeSheet(Long userId, LocalDate workDate, List<TimeSheetEntryCreateDTO> entriesDTO) {
        TimeSheet timeSheet = new TimeSheet();
        timeSheet.setUserId(userId);
        timeSheet.setWorkDate(workDate);
        timeSheet.setCreatedAt(LocalDateTime.now());
        timeSheet.setUpdatedAt(LocalDateTime.now());
        timeSheet.setStatus(TimeSheet.Status.DRAFT);

        WeekInfo weekInfo = findOrCreateWeekInfo(workDate);
        timeSheet.setWeekInfo(weekInfo);

        List<TimeSheetEntry> entries = entriesDTO.stream().map(dto -> {
            TimeSheetEntry entry = new TimeSheetEntry();
            entry.setTimeSheet(timeSheet);
            entry.setProjectId(dto.getProjectId());
            entry.setTaskId(dto.getTaskId());
            entry.setDescription(dto.getDescription());
            entry.setWorkLocation(dto.getWorkLocation());
            entry.setFromTime(dto.getFromTime());
            entry.setToTime(dto.getToTime());
            entry.setOtherDescription(dto.getOtherDescription());

            BigDecimal hours = dto.getHoursWorked();
            if (hours == null) {
                hours = calculateHours(dto.getFromTime(), dto.getToTime());
            }
            entry.setHoursWorked(hours);
            return entry;
        }).collect(Collectors.toList());

        timeSheet.setEntries(entries);
        timeSheet.setHoursWorked(entries.stream()
                .map(TimeSheetEntry::getHoursWorked)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        timeSheetRepository.save(timeSheet);

        return "Timesheet created successfully";
    }

    private BigDecimal calculateHours(LocalDateTime from, LocalDateTime to) {
    if (from == null || to == null) return BigDecimal.ZERO;
    if (to.isBefore(from)) throw new IllegalArgumentException("toTime cannot be before fromTime");

    Duration duration = Duration.between(from, to);
    long hours = duration.toHours();
    long minutes = duration.toMinutes() % 60;

    // Format as HH.MM where minutes are two digits
    String hhmm = String.format("%02d.%02d", hours, minutes);
    return new BigDecimal(hhmm);
    }


    private WeekInfo findOrCreateWeekInfo(LocalDate workDate) {
        return weekInfoRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(workDate, workDate)
                .orElseGet(() -> createWeekInfo(workDate));
    }

    private WeekInfo createWeekInfo(LocalDate date) {
        LocalDate startOfWeek = date.with(java.time.DayOfWeek.MONDAY);
        LocalDate endOfWeek = date.with(java.time.DayOfWeek.SUNDAY);

        WeekInfo weekInfo = new WeekInfo();
        weekInfo.setStartDate(startOfWeek);
        weekInfo.setEndDate(endOfWeek);
        weekInfo.setWeekNo(startOfWeek.get(WeekFields.ISO.weekOfYear()));
        weekInfo.setYear(startOfWeek.getYear());
        weekInfo.setMonth(startOfWeek.getMonthValue());
        weekInfo.setIncompleteWeek(false);

        return weekInfoRepository.save(weekInfo);
    }

    public String addEntriesToTimeSheet(AddEntryDTO addEntryDTO) {
        Long timesheetId = addEntryDTO.getTimeSheetId();

        if (timesheetId == null) {
            return "timeSheetId is required in the request body";
        }

        Optional<TimeSheet> optionalTimeSheet = timeSheetRepository.findById(timesheetId);

        if (optionalTimeSheet.isEmpty()) {
            return "No timesheet found with id: " + timesheetId;
        }

        TimeSheet timeSheet = optionalTimeSheet.get();

        if (addEntryDTO.getEntries() == null || addEntryDTO.getEntries().isEmpty()) {
            return "No entries provided to add";
        }

        // Add each entry
        for (TimeSheetEntryCreateDTO entryDTO : addEntryDTO.getEntries()) {
            TimeSheetEntry entry = new TimeSheetEntry();
            entry.setTimeSheet(timeSheet);
            entry.setProjectId(entryDTO.getProjectId());
            entry.setTaskId(entryDTO.getTaskId());
            entry.setDescription(entryDTO.getDescription());
            entry.setFromTime(entryDTO.getFromTime());
            entry.setToTime(entryDTO.getToTime());
            entry.setWorkLocation(entryDTO.getWorkLocation());
            entry.setHoursWorked(entryDTO.getHoursWorked());
            entry.setOtherDescription(entryDTO.getOtherDescription());

            timeSheet.getEntries().add(entry);
        }

        // Recalculate total hours
        timeSheet.setHoursWorked(
            timeSheet.getEntries().stream()
                .map(e -> e.getHoursWorked() != null ? e.getHoursWorked() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
        );

        timeSheet.setUpdatedAt(LocalDateTime.now());

        timeSheetRepository.save(timeSheet);

        return "Entries added successfully. Total hours now: " + timeSheet.getHoursWorked();
    }

}


