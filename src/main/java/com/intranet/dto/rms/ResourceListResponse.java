package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceListResponse {
    private boolean success;
    private String message;
    private List<ResourceDTO> data;
}
