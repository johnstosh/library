/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import com.muczynski.library.domain.Applied;

/**
 * JSON captured before and after an application change. The password is never
 * included.
 */
public record ApplicationMailSnapshot(
        Long id,
        String name,
        String email,
        String phone,
        String status) {

    public static ApplicationMailSnapshot from(Applied applied) {
        if (applied == null) {
            return null;
        }
        Applied.ApplicationStatus status = applied.getStatus();
        return new ApplicationMailSnapshot(
                applied.getId(),
                applied.getName(),
                applied.getEmail(),
                applied.getPhone(),
                status != null ? status.name() : null);
    }
}
