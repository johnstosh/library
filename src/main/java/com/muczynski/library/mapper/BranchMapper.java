/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.BranchDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BranchMapper {
    // Ignore "name" - it's only for backward-compatible JSON import, not entity mapping
    @Mapping(target = "name", ignore = true)
    BranchDto toDto(Library library);

    Library toEntity(BranchDto dto);
}
