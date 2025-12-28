/* (c) Copyright 2025 by Muczynski */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.User;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.mapper.UserMapper;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.util.PasswordHashingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

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

    private User findUserByUsername(String username) {
        List<User> users = userRepository.findAllByUsernameIgnoreCaseOrderByIdAsc(username);
        if (users.isEmpty()) {
            throw new LibraryException("User not found");
        }
        User user = users.get(0);
        if (users.size() > 1) {
            logger.warn("Found {} duplicate users with username '{}'. Using user with lowest ID: {}.",
                       users.size(), username, user.getId());
        }
        return user;
    }

    public UserDto getUserSettings(String currentUsername) {
        User user = findUserByUsername(currentUsername);
        return userMapper.toDto(user);
    }

    public UserDto updateUserSettings(String currentUsername, UserSettingsDto userSettingsDto) {
        User user = findUserByUsername(currentUsername);

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
            user.setPassword(passwordEncoder.encode(userSettingsDto.getPassword()));
        }

        // Always update API keys, even if empty (to allow clearing)
        if (userSettingsDto.getXaiApiKey() != null) {
            user.setXaiApiKey(userSettingsDto.getXaiApiKey());
        }

        if (userSettingsDto.getGooglePhotosApiKey() != null) {
            user.setGooglePhotosApiKey(userSettingsDto.getGooglePhotosApiKey());
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

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    public void deleteUser(String currentUsername) {
        User user = findUserByUsername(currentUsername);
        userRepository.delete(user);
    }
}
