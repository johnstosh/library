package com.muczynski.library.dto.importdtos;

import com.muczynski.library.domain.LibraryCardDesign;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportUserDto {
    private String userIdentifier;
    private String username;
    private String password;
    private String xaiApiKey = "";
    private String googlePhotosApiKey = "";
    private String googlePhotosRefreshToken = "";
    private String googlePhotosTokenExpiry = "";
    private String googleClientSecret = "";
    private String googlePhotosAlbumId = "";
    private String lastPhotoTimestamp = "";
    private String ssoProvider;
    private String ssoSubjectId;
    private String email;
    private LibraryCardDesign libraryCardDesign;
    private List<String> authorities = new ArrayList<>();
    private List<String> roles = new ArrayList<>(); // For backwards compatibility with older exports
}
