package com.intranet.controller.reports;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MockTimesheetFinanceReportController {

    @GetMapping("/monthly_finance")
    public ResponseEntity<Map<String, Object>> getMonthlyFinanceReport() {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("reportMonth", "November 2025");
        response.put("totalWorkingDays", 22);
        response.put("totalBillableHours", 3520);
        response.put("utilizationRate", "80%");

        // Hours Breakdown
        Map<String, Object> hoursBreakdown = new LinkedHashMap<>();
        hoursBreakdown.put("billableHours", 3520);
        hoursBreakdown.put("nonBillableHours", 880);
        hoursBreakdown.put("leaveHours", 176);
        hoursBreakdown.put("totalHours", 4576);
        response.put("hoursBreakdown", hoursBreakdown);

        // Employee Breakdown
        List<Map<String, Object>> employeeBreakdown = new ArrayList<>();
        employeeBreakdown.add(createEmployee("John Smith", 22, 160, 20, 8, 188, "95%"));
        employeeBreakdown.add(createEmployee("Sarah Johnson", 21, 148, 18, 12, 178, "91%"));
        employeeBreakdown.add(createEmployee("Mike Chen", 22, 152, 24, 4, 180, "94%"));
        employeeBreakdown.add(createEmployee("Emily Davis", 21, 145, 18, 13, 176, "93%"));
        employeeBreakdown.add(createEmployee("John Smith", 22, 160, 20, 8, 188, "95%"));
        employeeBreakdown.add(createEmployee("Sarah Johnson", 21, 148, 18, 12, 178, "91%"));
        employeeBreakdown.add(createEmployee("Mike Chen", 22, 152, 24, 4, 180, "94%"));
        employeeBreakdown.add(createEmployee("Emily Davis", 21, 145, 18, 13, 176, "93%"));
        employeeBreakdown.add(createEmployee("John Smith", 22, 160, 20, 8, 188, "95%"));
        employeeBreakdown.add(createEmployee("Sarah Johnson", 21, 148, 18, 12, 178, "91%"));
        employeeBreakdown.add(createEmployee("Mike Chen", 22, 152, 24, 4, 180, "94%"));
        employeeBreakdown.add(createEmployee("Emily Davis", 21, 145, 18, 13, 176, "93%"));
        response.put("employeeBreakdown", employeeBreakdown);

        // Employee Productivity
        List<Map<String, Object>> employeeProductivity = new ArrayList<>();
        employeeProductivity.add(createProductivity("EMP001", "John Smith", 22, 188, "95%"));
        employeeProductivity.add(createProductivity("EMP002", "Sarah Johnson", 21, 178, "91%"));
        employeeProductivity.add(createProductivity("EMP003", "Mike Chen", 22, 180, "94%"));
        employeeProductivity.add(createProductivity("EMP004", "Emily Davis", 21, 176, "93%"));
        employeeProductivity.add(createProductivity("EMP001", "John Smith", 22, 188, "95%"));
        employeeProductivity.add(createProductivity("EMP002", "Sarah Johnson", 21, 178, "91%"));
        employeeProductivity.add(createProductivity("EMP003", "Mike Chen", 22, 180, "94%"));
        employeeProductivity.add(createProductivity("EMP004", "Emily Davis", 21, 176, "93%"));
        employeeProductivity.add(createProductivity("EMP001", "John Smith", 22, 188, "95%"));
        employeeProductivity.add(createProductivity("EMP002", "Sarah Johnson", 21, 178, "91%"));
        employeeProductivity.add(createProductivity("EMP003", "Mike Chen", 22, 180, "94%"));
        employeeProductivity.add(createProductivity("EMP004", "Emily Davis", 21, 176, "93%"));
        employeeProductivity.add(createProductivity("EMP001", "John Smith", 22, 188, "95%"));
        employeeProductivity.add(createProductivity("EMP002", "Sarah Johnson", 21, 178, "91%"));
        employeeProductivity.add(createProductivity("EMP003", "Mike Chen", 22, 180, "94%"));
        employeeProductivity.add(createProductivity("EMP004", "Emily Davis", 21, 176, "93%"));
        response.put("employeeProductivity", employeeProductivity);

        // Project Breakdown
        List<Map<String, Object>> projectBreakdown = new ArrayList<>();
        projectBreakdown.add(createProject("Project Alpha", 3, 480));
        projectBreakdown.add(createProject("Project Beta", 2, 320));
        projectBreakdown.add(createProject("Project Gamma", 3, 240));
        projectBreakdown.add(createProject("Internal Training", 2, 0));
        projectBreakdown.add(createProject("Project Alpha", 3, 480));
        projectBreakdown.add(createProject("Project Beta", 2, 320));
        projectBreakdown.add(createProject("Project Gamma", 3, 240));
        projectBreakdown.add(createProject("Internal Training", 2, 0));
        projectBreakdown.add(createProject("Project Alpha", 3, 480));
        projectBreakdown.add(createProject("Project Beta", 2, 320));
        projectBreakdown.add(createProject("Project Gamma", 3, 240));
        projectBreakdown.add(createProject("Internal Training", 2, 0));
        projectBreakdown.add(createProject("Project Alpha", 3, 480));
        projectBreakdown.add(createProject("Project Beta", 2, 320));
        projectBreakdown.add(createProject("Project Gamma", 3, 240));
        projectBreakdown.add(createProject("Internal Training", 2, 0));
        response.put("projectBreakdown", projectBreakdown);

        return ResponseEntity.ok(response);
    }

    // Helper methods for cleaner code
    private Map<String, Object> createEmployee(String name, int workingDays, int billableHours,
                                               int nonBillableHours, int leaveHours,
                                               int totalHours, String productivity) {
        Map<String, Object> emp = new LinkedHashMap<>();
        emp.put("name", name);
        emp.put("workingDays", workingDays);
        emp.put("billableHours", billableHours);
        emp.put("nonBillableHours", nonBillableHours);
        emp.put("leaveHours", leaveHours);
        emp.put("totalHours", totalHours);
        emp.put("productivity", productivity);
        return emp;
    }

    private Map<String, Object> createProductivity(String id, String name, int days, int totalHours, String productivity) {
        Map<String, Object> prod = new LinkedHashMap<>();
        prod.put("employeeId", id);
        prod.put("employeeName", name);
        prod.put("workingDays", days);
        prod.put("totalHours", totalHours);
        prod.put("productivity", productivity);
        return prod;
    }

    private Map<String, Object> createProject(String name, int members, int totalHours) {
        Map<String, Object> project = new LinkedHashMap<>();
        project.put("projectName", name);
        project.put("teamMembers", members);
        project.put("totalHours", totalHours);
        return project;
    }
}
