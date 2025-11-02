/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSettingsDto {
    private String username;
    private String password;
    private String xaiApiKey;
    private String googlePhotosApiKey;
    private String googleClientSecret;
    private String lastPhotoTimestamp;
}