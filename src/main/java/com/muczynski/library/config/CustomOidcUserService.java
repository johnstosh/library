/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import com.muczynski.library.domain.Role;
import com.muczynski.library.domain.User;
import com.muczynski.library.repository.RoleRepository;
import com.muczynski.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("CustomOidcUserService.loadUser() called");

        // Get user info from OIDC provider (Google)
        OidcUser oidcUser = super.loadUser(userRequest);

        log.info("OidcUser loaded successfully, subject: {}", oidcUser.getSubject());

        // Extract user attributes
        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String subjectId = oidcUser.getSubject(); // Unique Google user ID
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        log.info("OIDC login attempt - Provider: {}, Email: {}, Subject ID: {}, Name: {}", provider, email, subjectId, name);

        // Find or create user
        java.util.List<User> users = userRepository.findAllBySsoProviderAndSsoSubjectIdOrderByIdAsc(provider, subjectId);
        User user;
        if (users.isEmpty()) {
            user = createNewSsoUser(provider, subjectId, email, name);
        } else {
            user = users.get(0); // Select the one with the lowest ID
            if (users.size() > 1) {
                log.warn("Found {} duplicate users for SSO provider '{}' and subject ID '{}'. Using user with lowest ID: {}.",
                         users.size(), provider, subjectId, user.getId());
            }
        }

        // Update email and name on each login (in case they changed)
        boolean updated = false;
        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            updated = true;
        }
        if (name != null && !name.equals(user.getUsername())) {
            // Only update username if it was set from email (not a custom name)
            if (user.getUsername() != null && user.getUsername().contains("@")) {
                user.setUsername(name);
                updated = true;
            }
        }
        if (updated) {
            userRepository.save(user);
            log.info("Updated user info for: {} (ID: {})", user.getUsername(), user.getId());
        }

        // Convert user roles to Spring Security authorities
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());

        log.info("User authenticated successfully: {} with roles: {}", user.getUsername(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")));

        // Return OidcUser with our database authorities
        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private User createNewSsoUser(String provider, String subjectId, String email, String name) {
        User user = new User();

        // Generate unique identifier (UUID) for this user
        user.setUserIdentifier(UUID.randomUUID().toString());

        // Use real name as username, or fallback to email, then provider_subjectId
        String username = name != null && !name.trim().isEmpty() ? name :
                         (email != null ? email : (provider + "_" + subjectId));
        user.setUsername(username);

        // SSO users don't have passwords
        user.setPassword(null);

        // Set SSO fields
        user.setSsoProvider(provider);
        user.setSsoSubjectId(subjectId);
        user.setEmail(email);

        // Initialize other fields with empty strings (to match existing user pattern)
        user.setXaiApiKey("");
        user.setGooglePhotosApiKey("");
        user.setGooglePhotosRefreshToken("");
        user.setGooglePhotosTokenExpiry("");
        user.setGoogleClientSecret("");
        user.setGooglePhotosAlbumId("");
        user.setLastPhotoTimestamp("");

        // Assign default USER role (create if doesn't exist)
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    log.info("USER role not found, creating it");
                    Role newRole = new Role();
                    newRole.setName("USER");
                    return roleRepository.save(newRole);
                });

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        // Save new user
        userRepository.save(user);

        log.info("Created new SSO user: {} (Provider: {}, Subject ID: {})", username, provider, subjectId);

        return user;
    }
}
