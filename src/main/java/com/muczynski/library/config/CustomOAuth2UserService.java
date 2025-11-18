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
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("CustomOAuth2UserService.loadUser() called");

        // Get user info from OAuth2 provider (Google)
        OAuth2User oauth2User = super.loadUser(userRequest);

        log.info("OAuth2User loaded successfully, attributes: {}", oauth2User.getAttributes());

        // Extract user attributes
        Map<String, Object> attributes = oauth2User.getAttributes();
        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String subjectId = (String) attributes.get("sub"); // Unique Google user ID
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        log.info("OAuth2 login attempt - Provider: {}, Email: {}, Subject ID: {}", provider, email, subjectId);

        // Find or create user
        // Use the list-based query to handle potential duplicates gracefully
        java.util.List<User> users = userRepository.findAllBySsoProviderAndSsoSubjectIdOrderByIdAsc(provider, subjectId);
        User user;
        if (users.isEmpty()) {
            user = createNewSsoUser(provider, subjectId, email, name);
        } else {
            user = users.get(0); // Select the one with the lowest ID
            if (users.size() > 1) {
                log.warn("Found {} duplicate users for SSO provider '{}' and subject ID '{}'. Using user with lowest ID: {}. " +
                         "Consider cleaning up duplicate entries in the database.",
                         users.size(), provider, subjectId, user.getId());
            }
        }

        // Update email on each login (in case it changed)
        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            userRepository.save(user);
            log.info("Updated email for user: {} (ID: {})", user.getUsername(), user.getId());
        }

        // Convert user roles to Spring Security authorities
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());

        log.info("User authenticated successfully: {} with roles: {}", user.getUsername(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")));

        // Return OAuth2User with authorities
        return new DefaultOAuth2User(authorities, attributes, "sub");
    }

    private User createNewSsoUser(String provider, String subjectId, String email, String name) {
        User user = new User();

        // Use email as username, or fallback to provider_subjectId if no email
        String username = email != null ? email : (provider + "_" + subjectId);
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
