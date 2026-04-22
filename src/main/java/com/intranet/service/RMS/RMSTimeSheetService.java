package com.intranet.service.RMS;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.intranet.dto.rms.RMSProjectHoursDTO;
import com.intranet.dto.rms.TimePeriodDataDTO;
import com.intranet.dto.rms.TimeSheetSummaryResponseDTO;
import com.intranet.repository.TimeSheetEntryRepo;
import com.intranet.repository.TimeSheetRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RMSTimeSheetService {

    private final TimeSheetRepo timeSheetRepository;
    private final TimeSheetEntryRepo entryRepository;

    public TimeSheetSummaryResponseDTO getSummary(
            LocalDate startDate,
            LocalDate endDate) {

        BigDecimal totalHours = timeSheetRepository.getTotalHoursForAllUsers(startDate, endDate);
        BigDecimal billableHours = entryRepository.getBillableHoursForAllUsers(startDate, endDate);
        BigDecimal nonBillableHours = entryRepository.getNonBillableHoursForAllUsers(startDate, endDate);
        List<RMSProjectHoursDTO> projectHours = entryRepository.getProjectHoursForAllUsers(startDate, endDate);
        Long totalUsers = timeSheetRepository.getUniqueUserCount(startDate, endDate);

        // Calculate averages
        BigDecimal averageTotalHours = totalUsers > 0 ? totalHours.divide(BigDecimal.valueOf(totalUsers), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal averageBillableHours = totalUsers > 0 ? billableHours.divide(BigDecimal.valueOf(totalUsers), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal averageNonBillableHours = totalUsers > 0 ? nonBillableHours.divide(BigDecimal.valueOf(totalUsers), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        TimeSheetSummaryResponseDTO response = new TimeSheetSummaryResponseDTO();
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setTotalHours(totalHours);
        response.setBillableHours(billableHours);
        response.setNonBillableHours(nonBillableHours);
        response.setProjectHours(projectHours);

        response.setTotalUsers(totalUsers);
        response.setAverageTotalHours(averageTotalHours);
        response.setAverageBillableHours(averageBillableHours);
        response.setAverageNonBillableHours(averageNonBillableHours);

        // Add daily, weekly, and monthly data
        response.setDaily(calculateDailyData(startDate, endDate));
        response.setWeekly(calculateWeeklyData(startDate, endDate));
        response.setMonthly(calculateMonthlyData(startDate, endDate));

        // Calculate additional metrics
        response.setTotalResources(totalUsers);
        response.setUtilization(calculateUtilization(totalHours, totalUsers, startDate, endDate));
        response.setBillableRatio(calculateBillableRatio(billableHours, totalHours));
        response.setConfidenceScore(calculateConfidenceScore(totalHours, totalUsers, startDate, endDate));

        return response;
    }

    private TimePeriodDataDTO createTimePeriodData(String period, BigDecimal actual, BigDecimal planned) {
        TimePeriodDataDTO data = new TimePeriodDataDTO();
        data.setPeriod(period);
        data.setActual(actual);
        data.setPlanned(planned);
        
        // Calculate utilization as percentage
        BigDecimal util = planned.compareTo(BigDecimal.ZERO) > 0 
            ? actual.divide(planned, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        data.setUtil(util);
        
        return data;
    }

    private List<TimePeriodDataDTO> calculateDailyData(LocalDate startDate, LocalDate endDate) {
        List<Object[]> dailyData = timeSheetRepository.getDailyHoursBreakdown(startDate, endDate);
        List<TimePeriodDataDTO> result = new ArrayList<>();
        
        // Default planned hours per day (8 hours)
        BigDecimal plannedHours = BigDecimal.valueOf(8);
        
        for (Object[] row : dailyData) {
            String period = (String) row[0];
            BigDecimal actual = (BigDecimal) row[1];
            result.add(createTimePeriodData(period, actual, plannedHours));
        }
        
        return result;
    }

    private List<TimePeriodDataDTO> calculateWeeklyData(LocalDate startDate, LocalDate endDate) {
        List<Object[]> weeklyData = timeSheetRepository.getWeeklyHoursBreakdown(startDate, endDate);
        List<TimePeriodDataDTO> result = new ArrayList<>();
        
        // Default planned hours per week (40 hours)
        BigDecimal plannedHours = BigDecimal.valueOf(40);
        
        for (Object[] row : weeklyData) {
            String period = (String) row[0];
            BigDecimal actual = (BigDecimal) row[1];
            result.add(createTimePeriodData(period, actual, plannedHours));
        }
        
        return result;
    }

    private List<TimePeriodDataDTO> calculateMonthlyData(LocalDate startDate, LocalDate endDate) {
        List<Object[]> monthlyData = timeSheetRepository.getMonthlyHoursBreakdown(startDate, endDate);
        List<TimePeriodDataDTO> result = new ArrayList<>();
        
        // Default planned hours per month (approximately 160 hours)
        BigDecimal plannedHours = BigDecimal.valueOf(160);
        
        for (Object[] row : monthlyData) {
            String period = (String) row[0];
            BigDecimal actual = (BigDecimal) row[1];
            result.add(createTimePeriodData(period, actual, plannedHours));
        }
        
        return result;
    }

    private BigDecimal calculateUtilization(BigDecimal totalHours, Long totalUsers, LocalDate startDate, LocalDate endDate) {
        if (totalUsers == 0 || totalHours.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculate working days between start and end date (excluding weekends)
        long workingDays = calculateWorkingDays(startDate, endDate);
        // Standard 8 hours per working day
        BigDecimal totalPlannedHours = BigDecimal.valueOf(workingDays * 8 * totalUsers);
        
        if (totalPlannedHours.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalHours.divide(totalPlannedHours, 4, RoundingMode.HALF_UP)
                       .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateBillableRatio(BigDecimal billableHours, BigDecimal totalHours) {
        if (totalHours.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return billableHours.divide(totalHours, 4, RoundingMode.HALF_UP)
                          .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateConfidenceScore(BigDecimal totalHours, Long totalUsers, LocalDate startDate, LocalDate endDate) {
        if (totalUsers == 0 || totalHours.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // Confidence score based on data completeness
        // Factors: total users, average hours per user, data consistency
        BigDecimal avgHoursPerUser = totalHours.divide(BigDecimal.valueOf(totalUsers), 2, RoundingMode.HALF_UP);
        
        // Base score: 50 points
        BigDecimal score = BigDecimal.valueOf(50);
        
        // Calculate weekly average for confidence scoring
        long weeks = calculateWeeksBetween(startDate, endDate);
        BigDecimal weeklyAvgHours = weeks > 0 ? avgHoursPerUser.divide(BigDecimal.valueOf(weeks), 2, RoundingMode.HALF_UP) : avgHoursPerUser;
        
        // Add points for good average (35-45 hours per week = standard full-time work)
        if (weeklyAvgHours.compareTo(BigDecimal.valueOf(35)) >= 0 && 
            weeklyAvgHours.compareTo(BigDecimal.valueOf(45)) <= 0) {
            score = score.add(BigDecimal.valueOf(30));
        } else if (weeklyAvgHours.compareTo(BigDecimal.valueOf(20)) >= 0) {
            score = score.add(BigDecimal.valueOf(15));
        }
        
        // Add points for user count (more users = more confidence)
        if (totalUsers >= 10) {
            score = score.add(BigDecimal.valueOf(20));
        } else if (totalUsers >= 5) {
            score = score.add(BigDecimal.valueOf(10));
        }
        
        // Cap at 100
        return score.min(BigDecimal.valueOf(100));
    }

    private long calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        long workingDays = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            int dayOfWeek = current.getDayOfWeek().getValue();
            // Monday=1, Friday=5 (exclude Saturday=6, Sunday=7)
            if (dayOfWeek <= 5) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }

    private long calculateWeeksBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return 0;
        }
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        return Math.max(1, (daysBetween / 7) + 1); // At least 1 week, partial weeks count as full weeks
    }
}
