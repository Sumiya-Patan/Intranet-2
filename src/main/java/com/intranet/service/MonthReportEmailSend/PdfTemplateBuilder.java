package com.intranet.service.MonthReportEmailSend;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.intranet.dto.LeavesAndHolidaysDTO;
import com.intranet.dto.MonthlyUserReportDTO;
import com.intranet.dto.WeekSummaryDTO;

@Service
public class PdfTemplateBuilder {

    public String buildUserMonthlyReportHtml(MonthlyUserReportDTO data) {

        String employeeName = data.getEmployeeName();
        String totalHours = data.getTotalHoursWorked().toString();
        String billable = data.getBillableHours().toString();
        String nonBillable = data.getNonBillableHours().toString();
        int activeProjects = data.getActiveProjectsCount();

        // Leaves & Holidays
        LeavesAndHolidaysDTO lh = data.getLeavesAndHolidays();

        // Daywise summary
        Map<String, Double> daywise = data.getDayWiseSummary();
        StringBuilder dayRows = new StringBuilder();

        daywise.forEach((day, hrs) -> {
            dayRows.append(String.format("""
                <tr>
                    <td>%s</td>
                    <td>%s</td>
                </tr>
            """, capitalize(day), hrs));
        });

        // Weekly Summary
        List<WeekSummaryDTO> weeklyList = data.getWeeklySummaryHistory();
        StringBuilder weeklyHtml = new StringBuilder();

        for (WeekSummaryDTO week : weeklyList) {

            weeklyHtml.append(String.format("""
            <div style="margin-top:30px; padding:10px; background:#fcf8e3; border-radius:6px;">
                <h3>
                    Week %s (%s to %s) — %s hrs 
                    <span style="color:#0d6efd; font-size:14px;">[Status: %s]</span>
                </h3>
            </div>
        """,
                week.getWeekId(),
                week.getStartDate(),
                week.getEndDate(),
                week.getTotalHours(),
                week.getWeeklyStatus()
        ));


            if (week.getTimesheets().isEmpty()) {
                weeklyHtml.append("""
                    <p style="color:gray;">No Timesheets Submitted</p>
                """);
                continue;
            }

            weeklyHtml.append("""
                <table style="width:100%; border-collapse:collapse; margin-top:10px;">
                    <tr>
                        <th>Date</th>
                        <th>Project</th>
                        <th>Task</th>
                        <th>Start</th>
                        <th>End</th>
                        <th>Hours</th>
                        <th>Billable</th>
                        <th>Description</th>
                    </tr>
            """);

            week.getTimesheets().forEach(ts -> {

                if (ts.getEntries().isEmpty()) {
                    weeklyHtml.append(String.format("""
                        <tr>
                            <td>%s</td>
                            <td colspan="7" style="text-align:center; color:#999;">Auto/Holiday Entry</td>
                        </tr>
                    """, ts.getWorkDate()));
                }

                ts.getEntries().forEach(e -> {
                    weeklyHtml.append(String.format("""
                        <tr>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                        </tr>
                    """,
                            ts.getWorkDate(),
                            e.getProjectId(),
                            e.getTaskId(),
                            e.getFromTime(),
                            e.getToTime(),
                            e.getHoursWorked(),
                            e.getIsBillable() ? "Yes" : "No",
                            e.getDescription()
                    ));
                });
            });

            weeklyHtml.append("</table>");
        }


        // Projects
        Map<String, Object> projectSummaries = data.getProjectSummaries();
        List<Map<String, Object>> projectList =
                (List<Map<String, Object>>) projectSummaries.get("projects");

        StringBuilder projectRows = new StringBuilder();
        for (Map<String, Object> p : projectList) {
            projectRows.append(String.format("""
                <tr>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s%%%%</td>
                </tr>
            """,
                    p.get("projectName"),
                    p.get("totalHours"),
                    p.get("billableHours"),
                    p.get("nonBillableHours"),
                    p.get("contribution")
            ));
        }

        return """
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #2c3e50; }
        table { width: 100%%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
        th { background: #f4f4f4; }
        .summary-box {
            padding: 15px;
            background: #eaf2f8;
            border-radius: 8px;
            margin-top: 20px;
        }
    </style>
</head>

<body>
    <h1>User Monthly Report</h1>

    <div class="summary-box">
        <h2>%s</h2>

        <p><strong>Total Hours Worked:</strong> %s</p>
        <p><strong>Billable Hours:</strong> %s</p>
        <p><strong>Non-Billable Hours:</strong> %s</p>
        <p><strong>Active Projects:</strong> %s</p>

        <hr/>

        <p><strong>Total Leaves:</strong> %s days (%s hrs)</p>
        <p><strong>Total Holidays:</strong> %s days</p>
    </div>

    <h2>Daywise Summary</h2>
    <table>
        <tr>
            <th>Day</th>
            <th>Hours</th>
        </tr>
        %s
    </table>

    <h2>Project Contributions</h2>
    <table>
        <tr>
            <th>Project</th>
            <th>Total Hrs</th>
            <th>Billable</th>
            <th>Non-Billable</th>
            <th>Contribution &#37;</th>
        </tr>
        %s
    </table>

    <h2>Weekly Summary</h2>
    %s

    <!-- REPORT NOTES SECTION -->
    <div style="margin-top:30px; padding:15px; background:#f7faff; border-left:6px solid #4a90e2; border-radius:6px;">
        <h2 style="margin-top:0;">Report Notes</h2>

        <ul style="font-size:12px; line-height:1.6; padding-left:18px;">
            <li><strong>Billable Hours</strong> – Total hours spent on tasks classified as billable across all projects.</li>

            <li><strong>Standard Holiday Hours</strong> – Calculated as 8 hours per holiday occurring between Monday and Friday.</li>

            <li><strong>Non-Billable Hours</strong> – Includes non-billable task hours + standard holiday hours.</li>

            <li><strong>Total Hours</strong> – Billable Hours + Non-Billable Hours.</li>

            <li><strong>Billable Utilization &#37;</strong> – Billable Hours ÷ Total Hours × 100.</li>

            <li><strong>Minimum Monthly Hours</strong> – The expected minimum contribution is 176 hours.</li>

            <li><strong>Leaves</strong> – Total number of approved leave days in this month.</li>

            <li><strong>Project-wise Hours Distribution &#37;</strong> – Shows your contribution to each project relative to total project hours.</li>
        </ul>
    </div>

    <p style="font-size:12px; color:#777; margin-top:40px; text-align:center;">
        Report generated by <strong>Timesheet Management System</strong> on %s
    </p>

</body>
</html>
""".formatted(
        employeeName,
        totalHours, billable, nonBillable, activeProjects,
        lh.getTotalLeavesDays(), lh.getTotalLeavesHours(),
        lh.getTotalHolidays(),
        dayRows,
        projectRows,
        weeklyHtml,
        LocalDateTime.now().toString()
);

    }


    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
