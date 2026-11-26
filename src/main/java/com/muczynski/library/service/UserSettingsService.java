/* (c) Copyright 2025 by Muczynski */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.SsoAccounts;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.email.EmailAddresses;
import com.muczynski.library.mapper.UserMapper;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.util.PasswordHashingUtil;
import com.muczynski.library.util.PhoneNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class UserSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailingListService mailingListService;

    @Transactional(readOnly = true)
    public UserDto getUserSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LibraryException("User not found"));
        return userMapper.toDto(user);
    }

    public UserDto updateUserSettings(Long userId, UserSettingsDto userSettingsDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LibraryException("User not found"));

        if (StringUtils.hasText(userSettingsDto.getUsername()) && !userSettingsDto.getUsername().equalsIgnoreCase(user.getUsername())) {
            if (!userRepository.findAllByUsernameIgnoreCaseOrderByIdAsc(userSettingsDto.getUsername()).isEmpty()) {
                throw new LibraryException("Username already taken");
            }
            user.setUsername(userSettingsDto.getUsername());
        }

        if (StringUtils.hasText(userSettingsDto.getPassword())) {
            // Validate password is SHA-256 hash from frontend
            if (!PasswordHashingUtil.isValidSHA256Hash(userSettingsDto.getPassword())) {
                throw new LibraryException("Invalid password format - expected SHA-256 hash");
            }

            // If changing password, verify current password if provided
            if (StringUtils.hasText(userSettingsDto.getCurrentPassword())) {
                if (!PasswordHashingUtil.isValidSHA256Hash(userSettingsDto.getCurrentPassword())) {
                    throw new LibraryException("Invalid current password format - expected SHA-256 hash");
                }
                // Verify current password matches
                if (!passwordEncoder.matches(userSettingsDto.getCurrentPassword(), user.getPassword())) {
                    throw new LibraryException("Current password is incorrect");
                }
            }

            user.setPassword(passwordEncoder.encode(userSettingsDto.getPassword()));
        }

        // Always update API keys, even if empty (to allow clearing)
        if (userSettingsDto.getXaiApiKey() != null) {
            user.setXaiApiKey(userSettingsDto.getXaiApiKey());
        }

        if (userSettingsDto.getGoogleApiKey() != null) {
            user.setGoogleApiKey(userSettingsDto.getGoogleApiKey());
        }

        applyAiProviderSelection(user, userSettingsDto);

        if (userSettingsDto.getGooglePhotosApiKey() != null) {
            user.setGooglePhotosApiKey(userSettingsDto.getGooglePhotosApiKey());
        }

        if (userSettingsDto.getGooglePhotosRefreshToken() != null) {
            user.setGooglePhotosRefreshToken(userSettingsDto.getGooglePhotosRefreshToken());
        }

        if (userSettingsDto.getGooglePhotosTokenExpiry() != null) {
            user.setGooglePhotosTokenExpiry(userSettingsDto.getGooglePhotosTokenExpiry());
        }

        if (userSettingsDto.getGoogleClientSecret() != null) {
            user.setGoogleClientSecret(userSettingsDto.getGoogleClientSecret());
        }

        if (userSettingsDto.getGooglePhotosAlbumId() != null) {
            user.setGooglePhotosAlbumId(userSettingsDto.getGooglePhotosAlbumId());
        }

        if (StringUtils.hasText(userSettingsDto.getLastPhotoTimestamp())) {
            user.setLastPhotoTimestamp(userSettingsDto.getLastPhotoTimestamp());
        }

        if (userSettingsDto.getLibraryCardDesign() != null) {
            user.setLibraryCardDesign(userSettingsDto.getLibraryCardDesign());
        }

        if (userSettingsDto.getEmail() != null) {
            String normalized = normalizeOptionalEmail(userSettingsDto.getEmail());
            if (SsoAccounts.isSso(user) && !sameEmail(user.getEmail(), normalized)) {
                throw new LibraryException("Email cannot be changed for an SSO user");
            }
            String previous = user.getEmail();
            if (!sameEmail(previous, normalized)) {
                mailingListService.moveEmail(previous, normalized);
            }
            user.setEmail(normalized);
        }

        if (userSettingsDto.getPhone() != null) {
            user.setPhone(normalizeOptionalPhone(userSettingsDto.getPhone()));
        }

        if (userSettingsDto.getReceiveEmails() != null) {
            user.setReceiveEmails(userSettingsDto.getReceiveEmails());
        }

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    /**
     * A null flag leaves the stored choice. Turning one provider on turns the other off.
     * Sending both true is rejected. Both false means no AI provider is selected.
     */
    private void applyAiProviderSelection(User user, UserSettingsDto userSettingsDto) {
        Boolean requestedXai = userSettingsDto.getUseXaiApiKey();
        Boolean requestedGoogle = userSettingsDto.getUseGoogleApiKey();
        if (requestedXai != null) {
            user.setUseXaiApiKey(requestedXai);
        }
        if (requestedGoogle != null) {
            user.setUseGoogleApiKey(requestedGoogle);
        }
        if (!user.isUseXaiApiKey() || !user.isUseGoogleApiKey()) {
            return;
        }
        if (Boolean.TRUE.equals(requestedXai) && Boolean.TRUE.equals(requestedGoogle)) {
            throw new LibraryException("Enable only one AI provider");
        }
        if (Boolean.TRUE.equals(requestedGoogle)) {
            user.setUseXaiApiKey(false);
            return;
        }
        if (Boolean.TRUE.equals(requestedXai)) {
            user.setUseGoogleApiKey(false);
            return;
        }
        throw new LibraryException("Enable only one AI provider");
    }

    private static boolean sameEmail(String current, String requested) {
        String left = current == null || current.isBlank() ? null : current;
        String right = requested == null || requested.isBlank() ? null : requested;
        return Objects.equals(left, right);
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String trimmed = email.trim();
        if (!EmailAddresses.isValid(trimmed)) {
            throw new LibraryException("Invalid email address: " + trimmed);
        }
        return trimmed;
    }

    private String normalizeOptionalPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim();
        if (!PhoneNumbers.isValid(trimmed)) {
            throw new LibraryException("Invalid phone number: " + trimmed);
        }
        return trimmed;
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LibraryException("User not found"));
        userRepository.delete(user);
    }
}
