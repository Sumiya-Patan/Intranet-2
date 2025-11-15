package com.intranet.controller.reports;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    @GetMapping("/sample")
    public ResponseEntity<Map<String, Object>> getSampleDashboard() {

        Map<String, Object> response = new HashMap<>();

        response.put("title", "Manager Dashboard — Team View");
        response.put("generatedDate", LocalDate.of(2025, 11, 14).toString());
        response.put("teamName", "Engineering Team Alpha");
        response.put("weekRange", "Nov 10 – Nov 16, 2025");

        // ---------------------- Summary Section ---------------------- //
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalHours", 187.5);
        summary.put("pendingApprovals", 7);
        summary.put("projectUtilization", 82.4);
        summary.put("topPerformer", "Sarah Chen");
        summary.put("topPerformerHours", 42.5);
        response.put("summary", summary);

        // ---------------------- Team Performance Chart ---------------------- //
        List<Map<String, Object>> performance = new ArrayList<>();
        performance.add(Map.of("name", "Sarah Chen", "hours", 42));
        performance.add(Map.of("name", "Marcus Johnson", "hours", 38));
        performance.add(Map.of("name", "Emily Rodriguez", "hours", 40));
        performance.add(Map.of("name", "David Kim", "hours", 35));
        performance.add(Map.of("name", "Lisa Patel", "hours", 32));
        response.put("teamPerformance", performance);

        // ---------------------- Productivity Pie ---------------------- //
        Map<String, Object> distribution = new LinkedHashMap<>();
        distribution.put("high", 50);
        distribution.put("medium", 30);
        distribution.put("low", 20);
        response.put("productivityDistribution", distribution);

        // ---------------------- Weekly Trend Line Chart ---------------------- //
        List<Map<String, Object>> weeklyTrend = new ArrayList<>();
        weeklyTrend.add(Map.of("week", "Week 41", "totalHours", 175));
        weeklyTrend.add(Map.of("week", "Week 42", "totalHours", 182));
        weeklyTrend.add(Map.of("week", "Week 43", "totalHours", 178));
        weeklyTrend.add(Map.of("week", "Week 44", "totalHours", 188));
        response.put("weeklyTrend", weeklyTrend);

        // ---------------------- Project Allocation ---------------------- //
        List<Map<String, Object>> projectAllocations = new ArrayList<>();

        projectAllocations.add(Map.of(
                "project", "Project Apollo",
                "members", Arrays.asList(
                        Map.of("name", "Sarah Chen", "hours", 20),
                        Map.of("name", "Marcus Johnson", "hours", 18),
                        Map.of("name", "Emily Rodriguez", "hours", 15)
                )
        ));

        projectAllocations.add(Map.of(
                "project", "Project Titan",
                "members", Arrays.asList(
                        Map.of("name", "Emily Rodriguez", "hours", 25),
                        Map.of("name", "David Kim", "hours", 22)
                )
        ));

        projectAllocations.add(Map.of(
                "project", "Project Nova",
                "members", Arrays.asList(
                        Map.of("name", "Sarah Chen", "hours", 22.5),
                        Map.of("name", "Lisa Patel", "hours", 20)
                )
        ));

        projectAllocations.add(Map.of(
                "project", "Project Orion",
                "members", Arrays.asList(
                        Map.of("name", "Marcus Johnson", "hours", 20),
                        Map.of("name", "David Kim", "hours", 13.5),
                        Map.of("name", "Lisa Patel", "hours", 11.5)
                )
        ));

        response.put("projectAllocation", projectAllocations);

        // ---------------------- Approval Queue ---------------------- //
        List<Map<String, Object>> approvals = new ArrayList<>();

        approvals.add(Map.of("employee", "Sarah Chen", "date", "2025-11-15", "hours", 8.5, "project", "Project Apollo", "status", "Pending"));
        approvals.add(Map.of("employee", "Marcus Johnson", "date", "2025-11-15", "hours", 8, "project", "Project Orion", "status", "Pending"));
        approvals.add(Map.of("employee", "Emily Rodriguez", "date", "2025-11-14", "hours", 9, "project", "Project Titan", "status", "Approved"));
        approvals.add(Map.of("employee", "David Kim", "date", "2025-11-15", "hours", 7.5, "project", "Project Titan", "status", "Pending"));
        approvals.add(Map.of("employee", "Lisa Patel", "date", "2025-11-15", "hours", 6.5, "project", "Project Nova", "status", "Pending"));

        response.put("approvalQueue", approvals);

        // ---------------------- Team Insights ---------------------- //
        Map<String, Object> insights = new LinkedHashMap<>();
        insights.put("underUtilized", Arrays.asList("David Kim (35.5 hrs)", "Lisa Patel (31.5 hrs)"));
        insights.put("overWorked", Arrays.asList("None identified"));
        insights.put("missedTimesheets", "All timesheets submitted");
        insights.put("recommendations", Arrays.asList(
                "Schedule capacity planning with 2 underutilized members",
                "7 pending approvals require immediate attention"
        ));
        response.put("teamInsights", insights);

        return ResponseEntity.ok(response);
    }
}
