/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.LibraryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LibraryMapper {
    // Ignore "name" - it's only for backward-compatible JSON import, not entity mapping
    @Mapping(target = "name", ignore = true)
    LibraryDto toDto(Library library);

    Library toEntity(LibraryDto dto);
}
