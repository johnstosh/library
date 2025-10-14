package com.muczynski.library.dto;

import lombok.Data;

@Data
public class CreateUserDto {
    private String username;
    private String password;
    private String role;
}
