/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.muczynski.library.domain.LibraryCardDesign;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSettingsDto {
    private String username;
    private String currentPassword;  // For password verification
    private String password;
    private String xaiApiKey;
    private String googlePhotosApiKey;
    private String googleClientSecret;
    private String googlePhotosAlbumId;
    private String lastPhotoTimestamp;
    private LibraryCardDesign libraryCardDesign;
}