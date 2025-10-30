package com.muczynski.library.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserImportDto {
    private String username;
    private String password;
    private String xaiApiKey = "";
    private List<String> roles = new ArrayList<>();
}
