/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String password;
    private Set<String> roles;
    private String xaiApiKey;
    private String googlePhotosApiKey;
    private String googleClientSecret;
    private String lastPhotoTimestamp;
    private int activeLoansCount;
}
