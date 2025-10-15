package com.intranet.service;

import com.intranet.dto.*;
import com.intranet.entity.TimeSheet;
import com.intranet.entity.WeekInfo;
import com.intranet.repository.TimeSheetRepo;
import com.intranet.repository.WeekInfoRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeeklySummaryService {

    private final TimeSheetRepo timeSheetRepo;
    private final WeekInfoRepo weekInfoRepo;

    public WeeklySummaryDTO getWeeklySummary(Long userId) {
        // 1️⃣ Find all weeks for the current month
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        List<WeekInfo> weeks = weekInfoRepo.findByStartDateGreaterThanEqualAndEndDateLessThanEqualOrderByStartDateAsc(startOfMonth, endOfMonth);

        // 2️⃣ Fetch timesheets for these weeks
        List<Long> weekIds = weeks.stream().map(WeekInfo::getId).collect(Collectors.toList());
        List<TimeSheet> timesheets = timeSheetRepo.findByUserIdAndWeekInfo_IdInOrderByWorkDateAsc(userId, weekIds);

        // 3️⃣ Map each week
        List<WeekSummaryDTO> weeklySummary = weeks.stream().map(week -> {
            List<TimeSheetSummaryDTO> weekTimesheets = timesheets.stream()
                    .filter(ts -> ts.getWeekInfo().getId().equals(week.getId()))
                    .map(this::mapTimeSheetToSummaryDTO)
                    .collect(Collectors.toList());

            WeekSummaryDTO weekDTO = new WeekSummaryDTO();
            weekDTO.setWeekId(week.getId());
            weekDTO.setStartDate(week.getStartDate());
            weekDTO.setEndDate(week.getEndDate());
            // ✅ Calculate total hours of this week
            BigDecimal totalHours = TimeUtil.sumHours(
                weekTimesheets.stream()
                    .map(TimeSheetSummaryDTO::getHoursWorked)
                    .collect(Collectors.toList())
            );
            weekDTO.setTotalHours(totalHours);
            weekDTO.setTimesheets(weekTimesheets);
            return weekDTO;
        }).collect(Collectors.toList());

        WeeklySummaryDTO summaryDTO = new WeeklySummaryDTO();
        summaryDTO.setUserId(userId);
        summaryDTO.setWeeklySummary(weeklySummary);

        return summaryDTO;
    }

    private TimeSheetSummaryDTO mapTimeSheetToSummaryDTO(TimeSheet ts) {
        List<TimeSheetEntrySummaryDTO> entries = ts.getEntries().stream().map(e -> {
            TimeSheetEntrySummaryDTO dto = new TimeSheetEntrySummaryDTO();
            dto.setProjectId(e.getProjectId());
            dto.setTaskId(e.getTaskId());
            dto.setDescription(e.getDescription());
            dto.setWorkLocation(e.getWorkLocation());
            dto.setFromTime(e.getFromTime() != null ? e.getFromTime().toLocalTime().toString() : null);
            dto.setToTime(e.getToTime() != null ? e.getToTime().toLocalTime().toString() : null);
            dto.setHoursWorked(e.getHoursWorked());
            dto.setOtherDescription(e.getOtherDescription());
            return dto;
        }).collect(Collectors.toList());

        TimeSheetSummaryDTO tsDTO = new TimeSheetSummaryDTO();
        tsDTO.setTimesheetId(ts.getId());
        tsDTO.setWorkDate(ts.getWorkDate());
        tsDTO.setHoursWorked(ts.getHoursWorked());
        tsDTO.setEntries(entries);
        return tsDTO;
    }
}
