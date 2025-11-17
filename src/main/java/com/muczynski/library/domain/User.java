/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255) default 'CLASSICAL_DEVOTION'")
    private LibraryCardDesign libraryCardDesign = LibraryCardDesign.CLASSICAL_DEVOTION;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
}