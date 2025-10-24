package com.muczynski.library.mapper;

import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.PhotoDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PhotoMapper {
    PhotoDto toDto(Photo photo);

    @Mapping(target = "image", ignore = true)
    @Mapping(target = "book", ignore = true)
    Photo toEntity(PhotoDto photoDto);
}
