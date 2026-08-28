/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.muczynski.library.domain.EmailMethod;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TestEmailResultDto {
    private boolean sent;
    private EmailMethod method;
    private String message;
    private List<String> recipients = new ArrayList<>();
}
