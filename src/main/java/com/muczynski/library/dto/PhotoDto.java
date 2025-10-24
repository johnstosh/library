package com.muczynski.library.dto;

import lombok.Data;

@Data
public class PhotoDto {
    private Long id;
    private String url;
    private String caption;
    private int rotation;
}
