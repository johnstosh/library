package com.muczynski.library.dto.importdtos;

import com.muczynski.library.dto.LibraryDto;
import lombok.Data;

import java.util.List;

@Data
public class ImportRequestDto {
    private List<LibraryDto> libraries;
    private List<ImportAuthorDto> authors;
    private List<ImportUserDto> users;
    private List<ImportBookDto> books;
    private List<ImportLoanDto> loans;
    private List<ImportPhotoDto> photos;
}
