/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import com.muczynski.library.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class UserCreatedAtIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void persistSetsCreatedAtAndLeavesItOnUpdate() {
        User user = new User();
        user.setUsername("created-at-" + UUID.randomUUID());
        user.setUserIdentifier(UUID.randomUUID().toString());
        user.setPassword("password");
        user.setSsoProvider("local");

        User saved = userRepository.saveAndFlush(user);
        assertNotNull(saved.getCreatedAt());
        LocalDateTime createdAt = saved.getCreatedAt();

        saved.setEmail("member@example.com");
        User updated = userRepository.saveAndFlush(saved);

        assertEquals(createdAt, updated.getCreatedAt());
        assertEquals("member@example.com", updated.getEmail());
    }
}
