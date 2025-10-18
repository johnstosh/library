package com.muczynski.library.mapper;

import com.muczynski.library.domain.Author;
import com.muczynski.library.dto.AuthorDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthorMapper {
    AuthorDto toDto(Author author);
    Author toEntity(AuthorDto authorDto);
}