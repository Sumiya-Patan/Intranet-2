package com.intranet.controller.reports;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MockTimesheetFinanceReportController {

     @GetMapping("/monthlyReport")
    public ResponseEntity<Map<String, Object>> getEmployeeSummary() {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("employeeId", 101);
        response.put("employeeName", "Sumiya Patan");
        response.put("totalHoursWorked", 76.00);
        response.put("billableHours", 64.00);
        response.put("nonBillableHours", 12.00);

        // Leaves & Holidays
        Map<String, Object> leavesAndHolidays = new LinkedHashMap<>();
        leavesAndHolidays.put("totalLeaves", 2);
        leavesAndHolidays.put("totalHolidays", 1);
        leavesAndHolidays.put("leaveDates", List.of("2025-11-06", "2025-11-07"));
        leavesAndHolidays.put("holidayDates", List.of("2025-11-14"));
        response.put("leavesAndHolidays", leavesAndHolidays);

        response.put("activeProjectsCount", 2);

        // Weekly Summary History
        List<Map<String, Object>> weeklyHistory = new ArrayList<>();

        // Week 5
        weeklyHistory.add(Map.of(
                "weekId", 5,
                "startDate", "2025-11-24",
                "endDate", "2025-11-30",
                "totalHours", 0,
                "weeklyStatus", "No Timesheets",
                "timesheets", List.of()
        ));

        // Week 4
        List<Map<String, Object>> week4Timesheets = new ArrayList<>();
        week4Timesheets.add(createTimesheet(21, "2025-11-18", 8.00,
                List.of(createEntry(4, 61, "Frontend fixes", true, 8.00))));
        week4Timesheets.add(createTimesheet(22, "2025-11-19", 8.00,
                List.of(createEntry(4, 64, "UI testing", true, 8.00))));
        week4Timesheets.add(createTimesheet(23, "2025-11-20", 12.00,
                List.of(
                        createEntry(4, 70, "API integration", true, 10.00),
                        createEntry(6, 71, "Internal training", false, 2.00)
                )));
        week4Timesheets.add(createTimesheet(24, "2025-11-21", 12.00,
                List.of(createEntry(6, 80, "Documentation", false, 12.00))));

        weeklyHistory.add(Map.of(
                "weekId", 4,
                "startDate", "2025-11-17",
                "endDate", "2025-11-23",
                "totalHours", 40.00,
                "weeklyStatus", "SUBMITTED",
                "timesheets", week4Timesheets
        ));

        // Week 3
        List<Map<String, Object>> week3Timesheets = new ArrayList<>();
        week3Timesheets.add(createTimesheet(15, "2025-11-10", 10.00,
                List.of(createEntry(4, 64, "Bug Fixing", true, 10.00))));
        week3Timesheets.add(createTimesheet(16, "2025-11-11", 8.00,
                List.of(createEntry(4, 66, "React Refactor", true, 8.00))));
        week3Timesheets.add(createTimesheet(17, "2025-11-12", 8.00,
                List.of(createEntry(6, 90, "Knowledge Sharing", false, 8.00))));
        week3Timesheets.add(createTimesheet(18, "2025-11-13", 10.00,
                List.of(createEntry(4, 68, "Backend Testing", true, 10.00))));

        weeklyHistory.add(Map.of(
                "weekId", 3,
                "startDate", "2025-11-10",
                "endDate", "2025-11-16",
                "totalHours", 36.00,
                "weeklyStatus", "SUBMITTED",
                "timesheets", week3Timesheets
        ));

        response.put("weeklySummaryHistory", weeklyHistory);

        // Day Wise Summary
        response.put("dayWiseSummary", Map.of(
                "monday", 10.00,
                "tuesday", 8.00,
                "wednesday", 8.00,
                "thursday", 10.00,
                "friday", 12.00,
                "saturday", 0.00,
                "sunday", 0.00
        ));

        // Project Summary
        List<Map<String, Object>> projectSummary = new ArrayList<>();
        projectSummary.add(Map.of(
                "projectId", 4,
                "projectName", "Project Atlas",
                "totalHoursWorked", 56.00,
                "billableHours", 54.00,
                "nonBillableHours", 2.00
        ));
        projectSummary.add(Map.of(
                "projectId", 6,
                "projectName", "Intranet Portal Revamp",
                "totalHoursWorked", 20.00,
                "billableHours", 10.00,
                "nonBillableHours", 10.00
        ));

        response.put("projectSummary", projectSummary);

        return ResponseEntity.ok(response);
    }

    // Helper: Create Timesheet
    private Map<String, Object> createTimesheet(int id, String date, double hours, List<Map<String, Object>> entries) {
        Map<String, Object> timesheet = new LinkedHashMap<>();
        timesheet.put("timesheetId", id);
        timesheet.put("workDate", date);
        timesheet.put("hoursWorked", hours);
        timesheet.put("status", "SUBMITTED");
        timesheet.put("entries", entries);
        return timesheet;
    }

    // Helper: Create Entry
    private Map<String, Object> createEntry(int projectId, int taskId, String desc, boolean billable, double hours) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("projectId", projectId);
        entry.put("taskId", taskId);
        entry.put("description", desc);
        entry.put("isBillable", billable);
        entry.put("hoursWorked", hours);
        return entry;
    }
    
}
