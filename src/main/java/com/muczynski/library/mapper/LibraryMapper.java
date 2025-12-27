/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.LibraryDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LibraryMapper {
    LibraryDto toDto(Library library);
    Library toEntity(LibraryDto dto);
}
