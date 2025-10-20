package com.muczynski.library.dto;

import lombok.Data;

@Data
public class UserSettingsDto {
    private String username;
    private String password;
    private String xaiApiKey;
}