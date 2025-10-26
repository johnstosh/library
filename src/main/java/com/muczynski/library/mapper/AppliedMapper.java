// (c) Copyright 2025 by Muczynski
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Applied;
import com.muczynski.library.dto.AppliedDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppliedMapper {

    AppliedDto appliedToAppliedDto(Applied applied);

    @Mapping(target = "password", ignore = true)
    Applied appliedDtoToApplied(AppliedDto appliedDto);
}