/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Optional override for the test-email endpoint. If {@code to} is blank, the
 * configured librarian recipients are used.
 */
@Getter
@Setter
public class TestEmailRequest {
    private String to;
}
