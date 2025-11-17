/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Role;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.UserDto;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setXaiApiKey(user.getXaiApiKey());
        userDto.setGooglePhotosApiKey(user.getGooglePhotosApiKey());
        userDto.setGoogleClientSecret(user.getGoogleClientSecret());
        userDto.setGooglePhotosAlbumId(user.getGooglePhotosAlbumId());
        userDto.setLastPhotoTimestamp(user.getLastPhotoTimestamp());
        userDto.setSsoProvider(user.getSsoProvider());
        userDto.setSsoSubjectId(user.getSsoSubjectId());
        userDto.setEmail(user.getEmail());
        userDto.setLibraryCardDesign(user.getLibraryCardDesign());
        userDto.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        return userDto;
    }
}
