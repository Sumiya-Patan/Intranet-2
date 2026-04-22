package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDTO {
    private Long resourceId;
    private String resourceName;
    private String resourceRole;
}