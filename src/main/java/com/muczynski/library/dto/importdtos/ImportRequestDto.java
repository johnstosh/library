/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.muczynski.library.dto.BranchDto;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)  // Use NON_NULL to preserve empty arrays in JSON
public class ImportRequestDto {
    @JsonProperty("libraries")  // Preserve "libraries" key for backward compatibility
    private List<BranchDto> branches;
    private List<ImportAuthorDto> authors;
    private List<ImportUserDto> users;
    private List<ImportBookDto> books;
    private List<ImportLoanDto> loans;
    private List<ImportPhotoDto> photos;
}
