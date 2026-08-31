/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void onCreateSetsCreatedAtAndLastModified() {
        User user = new User();
        user.onCreate();

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getLastModified());
        assertEquals(user.getCreatedAt(), user.getLastModified());
    }

    @Test
    void onCreateDoesNotOverwriteExistingCreatedAt() {
        User user = new User();
        LocalDateTime original = LocalDateTime.of(2024, 3, 15, 9, 0);
        user.setCreatedAt(original);

        user.onCreate();

        assertEquals(original, user.getCreatedAt());
        assertNotNull(user.getLastModified());
    }

    @Test
    void onUpdateChangesLastModifiedButNotCreatedAt() {
        User user = new User();
        user.onCreate();
        LocalDateTime createdAt = user.getCreatedAt();
        user.setLastModified(createdAt.minusSeconds(30));

        user.onUpdate();

        assertEquals(createdAt, user.getCreatedAt());
        assertTrue(user.getLastModified().isAfter(createdAt.minusSeconds(30)));
    }
}
