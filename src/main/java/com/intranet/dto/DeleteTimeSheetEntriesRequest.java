package com.intranet.dto;



import java.util.List;
import lombok.Data;

@Data
public class DeleteTimeSheetEntriesRequest {
    private Long timeSheetId;
    private List<Long> entryIds;
}
