/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.muczynski.library.domain.LibraryCardDesign;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"username", "password", "xaiApiKey", "googlePhotosApiKey",
    "googlePhotosRefreshToken", "googlePhotosTokenExpiry", "googleClientSecret",
    "googlePhotosAlbumId", "lastPhotoTimestamp", "ssoProvider", "ssoSubjectId",
    "email", "libraryCardDesign", "authorities", "roles", "userIdentifier"})
public class ImportUserDto {
    private String username;
    private String password;
    private String xaiApiKey;
    private String googlePhotosApiKey;
    private String googlePhotosRefreshToken;
    private String googlePhotosTokenExpiry;
    private String googleClientSecret;
    private String googlePhotosAlbumId;
    private String lastPhotoTimestamp;
    private String ssoProvider;
    private String ssoSubjectId;
    private String email;
    private LibraryCardDesign libraryCardDesign;
    private List<String> authorities = new ArrayList<>();
    private List<String> roles = new ArrayList<>(); // For backwards compatibility with older exports
    private String userIdentifier;  // Moved to end so it appears last in JSON
}
