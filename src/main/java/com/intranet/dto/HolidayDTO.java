package com.intranet.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

import com.intranet.dto.external.ManagerInfoDTO;

@Data
public class HolidayDTO {
    private Long holidayId;
    private String holidayName;
    private LocalDate holidayDate;
    private String holidayDescription;
    private String type;
    private String state;
    private String country;
    private Integer year;
    private boolean submitTimesheet;
    private String timeSheetReviews;
    private List<ManagerInfoDTO> allowedManagers; // ✅ new field

}
