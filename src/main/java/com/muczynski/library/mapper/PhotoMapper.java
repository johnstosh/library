/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.PhotoDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PhotoMapper {
    @Mapping(source = "book.id", target = "bookId")
    @Mapping(source = "author.id", target = "authorId")
    PhotoDto toDto(Photo photo);

    @Mapping(target = "image", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "photoOrder", ignore = true)
    @Mapping(target = "permanentId", ignore = true)
    @Mapping(target = "backedUpAt", ignore = true)
    @Mapping(target = "backupStatus", ignore = true)
    @Mapping(target = "backupErrorMessage", ignore = true)
    Photo toEntity(PhotoDto photoDto);
}
