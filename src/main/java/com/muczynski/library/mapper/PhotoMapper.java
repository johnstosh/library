package com.muczynski.library.mapper;

import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.PhotoDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PhotoMapper {
    PhotoDto toDto(Photo photo);
    Photo toEntity(PhotoDto photoDto);
}