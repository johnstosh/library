package com.muczynski.library.dto.importdtos;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportUserDto {
    private String username;
    private String password;
    private String xaiApiKey = "";
    private List<String> roles = new ArrayList<>();
}
