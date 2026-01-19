/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CustomOAuth2User to verify it returns database user ID as principal name
 */
class CustomOAuth2UserTest {

    @Test
    void getName_returnsUserIdInsteadOfSubject() {
        // Arrange
        Long databaseUserId = 42L;
        String oauthSubjectId = "google-oauth-subject-12345";

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", oauthSubjectId);
        attributes.put("email", "test@example.com");
        attributes.put("name", "Test User");

        Set<SimpleGrantedAuthority> authorities = Set.of(
                new SimpleGrantedAuthority("USER")
        );

        // Act
        CustomOAuth2User user = new CustomOAuth2User(authorities, attributes, "sub", databaseUserId);

        // Assert - getName should return database user ID, not OAuth subject
        assertEquals("42", user.getName());
        assertNotEquals(oauthSubjectId, user.getName());
    }

    @Test
    void getUserId_returnsDatabaseUserId() {
        // Arrange
        Long databaseUserId = 123L;

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "oauth-subject");

        Set<SimpleGrantedAuthority> authorities = Set.of(
                new SimpleGrantedAuthority("USER")
        );

        // Act
        CustomOAuth2User user = new CustomOAuth2User(authorities, attributes, "sub", databaseUserId);

        // Assert
        assertEquals(123L, user.getUserId());
    }

    @Test
    void getAttributes_returnsOriginalAttributes() {
        // Arrange
        Long databaseUserId = 1L;

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "oauth-subject");
        attributes.put("email", "test@example.com");
        attributes.put("name", "Test User");

        Set<SimpleGrantedAuthority> authorities = Set.of(
                new SimpleGrantedAuthority("USER")
        );

        // Act
        CustomOAuth2User user = new CustomOAuth2User(authorities, attributes, "sub", databaseUserId);

        // Assert - attributes should still be accessible
        assertEquals("test@example.com", user.getAttributes().get("email"));
        assertEquals("Test User", user.getAttributes().get("name"));
    }

    @Test
    void getAuthorities_returnsGrantedAuthorities() {
        // Arrange
        Long databaseUserId = 1L;

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "oauth-subject");

        Set<SimpleGrantedAuthority> authorities = Set.of(
                new SimpleGrantedAuthority("USER"),
                new SimpleGrantedAuthority("LIBRARIAN")
        );

        // Act
        CustomOAuth2User user = new CustomOAuth2User(authorities, attributes, "sub", databaseUserId);

        // Assert
        assertEquals(2, user.getAuthorities().size());
        assertTrue(user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("USER")));
        assertTrue(user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("LIBRARIAN")));
    }
}
