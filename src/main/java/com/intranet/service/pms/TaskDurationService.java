package com.intranet.service.pms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.intranet.dto.pms.ProjectUserRequestDTO;
import com.intranet.dto.pms.TaskDurationDTO;
import com.intranet.dto.pms.TaskDurationResponseDTO;
import com.intranet.repository.TimeSheetEntryRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskDurationService {

    private final TimeSheetEntryRepo repository;

    private String convertHoursToHHMM(BigDecimal hours) {
        int wholeHours = hours.intValue();

        BigDecimal decimalPart = hours.remainder(BigDecimal.ONE);
        int minutes = decimalPart.multiply(BigDecimal.valueOf(100)).intValue();

        wholeHours += minutes / 60;
        minutes = minutes % 60;

        return String.format("%d:%02d", wholeHours, minutes);
    }

    public List<TaskDurationDTO> getTaskDurations(Long projectId, Long userId) {
        List<Object[]> results =
                repository.findTaskDurationsByProjectAndUser(projectId, userId);

        List<TaskDurationDTO> response = new ArrayList<>();

        for (Object[] row : results) {
            Long taskId = (Long) row[0];
            BigDecimal totalHours = (BigDecimal) row[1];

            String duration = convertHoursToHHMM(totalHours);

            response.add(new TaskDurationDTO(taskId, duration));
        }
        return response;
    }

    public List<TaskDurationDTO> getTaskDurations(Long projectId,
                                                  Long userId,
                                                  LocalDate startDate,
                                                  LocalDate endDate) {

        List<Object[]> results =
                repository.findTaskDurationsByProjectAndUserAndDateRange(
                        projectId, userId, startDate, endDate);

        List<TaskDurationDTO> response = new ArrayList<>();

        for (Object[] row : results) {
            Long taskId = (Long) row[0];
            BigDecimal totalHours = (BigDecimal) row[1];

            String duration = convertHoursToHHMM(totalHours);

            response.add(new TaskDurationDTO(taskId, duration));
        }

        return response;
    }

    public List<TaskDurationResponseDTO> getTaskDurationsForUsers(ProjectUserRequestDTO dto) {

        List<TaskDurationResponseDTO> responseList = new ArrayList<>();

        for (Long userId : dto.getUserIds()) {

            List<Object[]> results =
                    repository.findTaskDurationsByProjectAndUser(dto.getProjectId(), userId);

            List<TaskDurationDTO> taskDurations = new ArrayList<>();

            for (Object[] row : results) {
                Long taskId = (Long) row[0];
                BigDecimal totalHours = (BigDecimal) row[1];

                String duration = convertHoursToHHMM(totalHours);

                taskDurations.add(new TaskDurationDTO(taskId, duration));
            }

            responseList.add(new TaskDurationResponseDTO(userId, taskDurations));
        }
        return responseList;
    }


    public List<TaskDurationResponseDTO> getTaskDurationsForUsersWithDateRange(ProjectUserRequestDTO dto, LocalDate startDate, LocalDate endDate) {

        List<TaskDurationResponseDTO> responseList = new ArrayList<>();

        for (Long userId : dto.getUserIds()) {

            List<Object[]> results =
                    repository.findTaskDurationsByUserProjectAndDateRange(
                            userId,
                            dto.getProjectId(),
                            startDate,
                            endDate
                    );

            List<TaskDurationDTO> taskDurations = new ArrayList<>();

            for (Object[] row : results) {
                Long taskId = (Long) row[0];
                BigDecimal totalHours = (BigDecimal) row[1];

                String duration = convertHoursToHHMM(totalHours);
                taskDurations.add(new TaskDurationDTO(taskId, duration));
            }

            responseList.add(new TaskDurationResponseDTO(userId, taskDurations));
        }
        return responseList;
    }
}
