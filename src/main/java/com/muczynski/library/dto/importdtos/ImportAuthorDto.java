/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ImportAuthorDto {
    private String name;
    private LocalDate dateOfBirth;
    private LocalDate dateOfDeath;
    private String religiousAffiliation;
    private String birthCountry;
    private String nationality;
    private String briefBiography;
    private String grokipediaUrl;
}
