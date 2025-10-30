package com.muczynski.library.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class AuthorImportDto {
    private String name;
    private LocalDate dateOfBirth;
    private LocalDate dateOfDeath;
    private String religiousAffiliation;
    private String birthCountry;
    private String nationality;
    private String briefBiography;
    private List<PhotoImportDto> photos = new ArrayList<>();
}
