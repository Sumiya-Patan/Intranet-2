package com.intranet.dto.email;

import lombok.Data;

@Data
public class MissingTimesheetEmailDTO {
    private String userName;
    private String startDate;
    private String endDate;
}
