package com.muczynski.library.dto;

import com.muczynski.library.domain.Applied;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AppliedDto {
    private Long id;
    private String name;
    private Applied.ApplicationStatus status;
}