// (c) Copyright 2025 by Muczynski
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserDto {
    private Long id;
    private String username;
    private String authority; // "LIBRARIAN" or "USER"
    private String ssoSubjectId;
    private LocalDateTime createdAt;
}
