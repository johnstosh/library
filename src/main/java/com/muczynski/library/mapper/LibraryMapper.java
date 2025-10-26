// (c) Copyright 2025 by Muczynski
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.LibraryDto;
import org.springframework.stereotype.Component;

@Component
public class LibraryMapper {

    public LibraryDto toDto(Library library) {
        LibraryDto libraryDto = new LibraryDto();
        libraryDto.setId(library.getId());
        libraryDto.setName(library.getName());
        libraryDto.setHostname(library.getHostname());
        return libraryDto;
    }

    public Library toEntity(LibraryDto libraryDto) {
        Library library = new Library();
        library.setId(libraryDto.getId());
        library.setName(libraryDto.getName());
        library.setHostname(libraryDto.getHostname());
        return library;
    }
}