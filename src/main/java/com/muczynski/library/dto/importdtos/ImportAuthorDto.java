package com.muczynski.library.dto.importdtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ImportAuthorDto {
    private String name;
    private LocalDate dateOfBirth;
    private LocalDate dateOfDeath;
    private String religiousAffiliation;
    private String birthCountry;
    private String nationality;
    private String briefBiography;
}
