// (c) Copyright 2025 by Muczynski
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.AuthorDto;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Comparator;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

    @Mapping(target = "firstPhotoId", ignore = true)
    @Mapping(target = "firstPhotoRotation", ignore = true)
    AuthorDto toDto(Author author);

    @Mapping(target = "photos", ignore = true)
    Author toEntity(AuthorDto authorDto);

    @AfterMapping
    default void afterToDto(Author author, @MappingTarget AuthorDto dto) {
        author.getPhotos().stream()
                .min(Comparator.comparing(Photo::getPhotoOrder))
                .ifPresent(photo -> {
                    dto.setFirstPhotoId(photo.getId());
                    dto.setFirstPhotoRotation(photo.getRotation());
                });
    }
}