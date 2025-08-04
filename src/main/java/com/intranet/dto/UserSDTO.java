package com.intranet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSDTO {

    private Long userId;
    private String name;
    private String email;
    
}
