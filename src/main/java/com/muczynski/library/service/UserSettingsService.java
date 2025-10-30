package com.muczynski.library.service;

import com.muczynski.library.domain.User;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.mapper.UserMapper;
import com.muczynski.library.repository.UserRepository;
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
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.toDto(user);
    }

    public UserDto updateUserSettings(String currentUsername, UserSettingsDto userSettingsDto) {
        User user = userRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (StringUtils.hasText(userSettingsDto.getUsername()) && !userSettingsDto.getUsername().equalsIgnoreCase(user.getUsername())) {
            if (userRepository.findByUsernameIgnoreCase(userSettingsDto.getUsername()).isPresent()) {
                throw new RuntimeException("Username already taken");
            }
            user.setUsername(userSettingsDto.getUsername());
        }

        if (StringUtils.hasText(userSettingsDto.getPassword())) {
            user.setPassword(passwordEncoder.encode(userSettingsDto.getPassword()));
        }

        if (StringUtils.hasText(userSettingsDto.getXaiApiKey())) {
            user.setXaiApiKey(userSettingsDto.getXaiApiKey());
        }

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    public void deleteUser(String currentUsername) {
        User user = userRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }
}
