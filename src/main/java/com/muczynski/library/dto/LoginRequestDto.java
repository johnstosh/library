// (c) Copyright 2025 by Muczynski
package com.muczynski.library.dto;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String username;
    private String password; // SHA-256 hashed password from frontend
}
