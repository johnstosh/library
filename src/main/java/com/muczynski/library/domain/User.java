/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique identifier for the user (UUID) - used for identification across systems
    @Column(unique = true)
    private String userIdentifier;

    private String username;

    @Column(length = 60)
    private String password;

    private String xaiApiKey = "";
    private String googlePhotosApiKey = ""; // OAuth access token
    private String googlePhotosRefreshToken = ""; // OAuth refresh token
    private String googlePhotosTokenExpiry = ""; // ISO 8601 timestamp
    private String googleClientSecret = ""; // Google OAuth client secret
    private String googlePhotosAlbumId = ""; // Permanent album ID for photo export
    private String lastPhotoTimestamp = "";

    // SSO fields
    private String ssoProvider; // "google", "local", or null for legacy users
    private String ssoSubjectId; // OAuth "sub" claim - unique user ID from provider
    private String email; // Email address from SSO provider

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255) default 'CLASSICAL_DEVOTION'")
    private LibraryCardDesign libraryCardDesign = LibraryCardDesign.CLASSICAL_DEVOTION;

    private LocalDateTime lastModified;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Authority> authorities;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }

    /**
     * Get the highest authority for this user.
     * Returns "LIBRARIAN" if user has LIBRARIAN authority, otherwise "USER".
     */
    public String getHighestAuthority() {
        if (authorities == null || authorities.isEmpty()) {
            return "USER";
        }
        return authorities.stream()
                .anyMatch(authority -> "LIBRARIAN".equals(authority.getName())) ? "LIBRARIAN" : "USER";
    }
}