package com.intranet.dto;



import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TimeSheetUpdateRequest {
    private List<EntryUpdateDto> entries;

    @Data
    public static class EntryUpdateDto {
        private Long id; // existing entry id
        private Long projectId;
        private Long taskId;
        private String description;
        private String workLocation;
        private LocalDateTime fromTime;
        private LocalDateTime toTime;
        private Double hoursWorked;
        private String otherDescription;
    }
}
