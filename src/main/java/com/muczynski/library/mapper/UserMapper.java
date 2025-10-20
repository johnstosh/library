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
        userDto.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        userDto.setXaiApiKey(user.getXaiApiKey());
        return userDto;
    }
}