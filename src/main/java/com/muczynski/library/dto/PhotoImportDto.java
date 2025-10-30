package com.muczynski.library.dto;

import lombok.Data;

@Data
public class PhotoImportDto {
    private String contentType;
    private String imageBase64;
    private String caption;
}
