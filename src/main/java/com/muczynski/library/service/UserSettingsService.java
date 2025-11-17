package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.User;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.mapper.UserMapper;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.util.PasswordHashingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class UserSettingsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    public UserDto getUserSettings(String currentUsername) {
        User user = userRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new LibraryException("User not found"));
        return userMapper.toDto(user);
    }

    public UserDto updateUserSettings(String currentUsername, UserSettingsDto userSettingsDto) {
        User user = userRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new LibraryException("User not found"));

        if (StringUtils.hasText(userSettingsDto.getUsername()) && !userSettingsDto.getUsername().equalsIgnoreCase(user.getUsername())) {
            if (userRepository.findByUsernameIgnoreCase(userSettingsDto.getUsername()).isPresent()) {
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
        User user = userRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new LibraryException("User not found"));
        userRepository.delete(user);
    }
}
