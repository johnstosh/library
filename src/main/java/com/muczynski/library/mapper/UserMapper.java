/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Authority;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "authorities", source = "authorities", qualifiedByName = "authoritiesToStrings")
    @Mapping(target = "activeLoansCount", ignore = true) // Set by service
    @Mapping(target = "password", ignore = true) // Never expose password in DTO
    UserDto toDto(User user);

    @Named("authoritiesToStrings")
    default Set<String> authoritiesToStrings(Set<Authority> authorities) {
        if (authorities == null) {
            return null;
        }
        return authorities.stream()
                .map(Authority::getName)
                .collect(Collectors.toSet());
    }
}
