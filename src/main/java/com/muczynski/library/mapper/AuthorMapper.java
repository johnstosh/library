package com.muczynski.library.mapper;

import com.muczynski.library.domain.Author;
import com.muczynski.library.dto.AuthorDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthorMapper {
    AuthorDto toDto(Author author);
    @Mapping(target = "photos", ignore = true)
    Author toEntity(AuthorDto authorDto);
}