/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import com.muczynski.library.repository.AuthorityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Component
public class RandomUser {

    @Autowired
    private AuthorityRepository authorityRepository;

    private static final List<String> FIRST_NAMES = List.of(
            "John", "Jane", "Michael", "Sarah", "David",
            "Emily", "Robert", "Lisa", "Thomas", "Maria"
    );

    private static final List<String> LAST_NAMES = List.of(
            "Anderson", "Baker", "Clark", "Davis", "Evans",
            "Fisher", "Green", "Harris", "Jackson", "King"
    );

    private static final Random RANDOM = new Random();

    public User create() {
        User user = new User();
        String firstName = FIRST_NAMES.get(RANDOM.nextInt(FIRST_NAMES.size()));
        String lastName = LAST_NAMES.get(RANDOM.nextInt(LAST_NAMES.size()));

        // Create unique username with test-data prefix for easy identification
        String username = "test-data-" + firstName.toLowerCase() + "." + lastName.toLowerCase() + RANDOM.nextInt(1000);
        user.setUsername(username);

        // Set a simple password (SHA-256 hash of "password123")
        // In real usage, password would be hashed on client side before sending
        user.setPassword("ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f");

        // Generate unique user identifier
        user.setUserIdentifier(UUID.randomUUID().toString());

        // Assign USER authority (most test users are regular users)
        // 20% chance of being a librarian
        String authorityName = RANDOM.nextInt(100) < 20 ? "LIBRARIAN" : "USER";
        List<Authority> existingAuthorities = authorityRepository.findAllByNameOrderByIdAsc(authorityName);
        Authority authority;
        if (!existingAuthorities.isEmpty()) {
            authority = existingAuthorities.get(0);
        } else {
            Authority newAuthority = new Authority();
            newAuthority.setName(authorityName);
            authority = authorityRepository.save(newAuthority);
        }

        Set<Authority> authorities = new HashSet<>();
        authorities.add(authority);
        user.setAuthorities(authorities);

        // Set default library card design
        user.setLibraryCardDesign(LibraryCardDesign.CLASSICAL_DEVOTION);

        // Mark as local (non-SSO) user
        user.setSsoProvider("local");

        return user;
    }
}
