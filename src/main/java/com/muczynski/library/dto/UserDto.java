package com.muczynski.library.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String password;
    private Set<String> roles;
}