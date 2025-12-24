/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AuthorDto {
    private Long id;

    @NotBlank(message = "Author name is required")
    private String name;

    private LocalDate dateOfBirth;
    private LocalDate dateOfDeath;
    private String religiousAffiliation;
    private String birthCountry;
    private String nationality;
    private String briefBiography;
    private Long firstPhotoId;
    private String firstPhotoChecksum;
    private Long bookCount;
    private LocalDateTime lastModified;
}
