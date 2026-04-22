package com.intranet.dto.rms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KPIStatDTO {
    private String label;
    private String value;
    private String trend;
}
