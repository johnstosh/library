// (c) Copyright 2025 by Muczynski
package com.muczynski.library.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AuthorDto {
    private Long id;
    private String name;
    private LocalDate dateOfBirth;
    private LocalDate dateOfDeath;
    private String religiousAffiliation;
    private String birthCountry;
    private String nationality;
    private String briefBiography;
}