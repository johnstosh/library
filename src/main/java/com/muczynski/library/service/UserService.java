/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.Applied;
import com.muczynski.library.domain.Role;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.CreateUserDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.mapper.UserMapper;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.RoleRepository;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.util.PasswordHashingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UserDto> getAllUsers() {
        List<UserDto> userDtos = userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
        for (UserDto dto : userDtos) {
            dto.setActiveLoansCount((int) loanRepository.countByUserIdAndReturnDateIsNull(dto.getId()));
        }
        return userDtos;
    }

    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    UserDto dto = userMapper.toDto(user);
                    dto.setActiveLoansCount((int) loanRepository.countByUserIdAndReturnDateIsNull(id));
                    return dto;
                })
                .orElse(null);
    }

    public UserDto getUserByUsername(String username) {
        // Use list-based query to handle potential duplicates gracefully
        List<User> users = userRepository.findAllByUsernameOrderByIdAsc(username);
        if (users.isEmpty()) {
            return null;
        }
        User user = users.get(0); // Select the one with the lowest ID
        if (users.size() > 1) {
            logger.warn("Found {} duplicate users with username '{}'. Using user with lowest ID: {}. " +
                       "Consider cleaning up duplicate entries in the database.",
                       users.size(), username, user.getId());
        }
        UserDto dto = userMapper.toDto(user);
        dto.setActiveLoansCount((int) loanRepository.countByUserIdAndReturnDateIsNull(user.getId()));
        return dto;
    }

    public UserDto createUser(CreateUserDto dto) {
        if (!userRepository.findAllByUsernameOrderByIdAsc(dto.getUsername()).isEmpty()) {
            throw new LibraryException("Username already exists");
        }

        // Validate password is SHA-256 hash from frontend
        if (!PasswordHashingUtil.isValidSHA256Hash(dto.getPassword())) {
            throw new LibraryException("Invalid password format - expected SHA-256 hash");
        }

        User user = new User();
        user.setUserIdentifier(UUID.randomUUID().toString()); // Generate unique identifier
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setSsoProvider("local"); // Mark as local (non-SSO) user

        Role role = roleRepository.findByName(dto.getRole()).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(dto.getRole());
            return roleRepository.save(newRole);
        });
        user.setRoles(Collections.singleton(role));

        User savedUser = userRepository.save(user);
        UserDto dtoResponse = userMapper.toDto(savedUser);
        dtoResponse.setActiveLoansCount(0); // New user has no loans
        return dtoResponse;
    }

    public UserDto createUserFromApplied(Applied applied) {
        if (!userRepository.findAllByUsernameOrderByIdAsc(applied.getName()).isEmpty()) {
            throw new LibraryException("Username already exists");
        }

        User user = new User();
        user.setUserIdentifier(UUID.randomUUID().toString()); // Generate unique identifier
        user.setUsername(applied.getName());
        user.setPassword(applied.getPassword()); // Already encoded from Applied creation
        user.setSsoProvider("local"); // Mark as local (non-SSO) user

        Role role = roleRepository.findByName("USER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("USER");
            return roleRepository.save(newRole);
        });
        user.setRoles(Collections.singleton(role));

        User savedUser = userRepository.save(user);
        UserDto dtoResponse = userMapper.toDto(savedUser);
        dtoResponse.setActiveLoansCount(0); // New user has no loans
        return dtoResponse;
    }

    public UserDto updateUser(Long id, CreateUserDto dto) {
        User user = userRepository.findById(id).orElseThrow(() -> new LibraryException("User not found: " + id));

        // Check if user is SSO user
        boolean isSsoUser = user.getSsoProvider() != null && !user.getSsoProvider().equals("local");

        if (dto.getUsername() != null && !dto.getUsername().isEmpty() && !dto.getUsername().equals(user.getUsername())) {
            if (!userRepository.findAllByUsernameOrderByIdAsc(dto.getUsername()).isEmpty()) {
                throw new LibraryException("Username already exists");
            }
            user.setUsername(dto.getUsername());
        }
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            // Prevent password updates for SSO users
            if (isSsoUser) {
                throw new LibraryException("Cannot update password for SSO user");
            }
            // Validate password is SHA-256 hash from frontend
            if (!PasswordHashingUtil.isValidSHA256Hash(dto.getPassword())) {
                throw new LibraryException("Invalid password format - expected SHA-256 hash");
            }
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getRole() != null && !dto.getRole().isEmpty()) {
            Role role = roleRepository.findByName(dto.getRole()).orElseGet(() -> {
                Role newRole = new Role();
                newRole.setName(dto.getRole());
                return roleRepository.save(newRole);
            });
            user.getRoles().clear();
            user.getRoles().add(role);
        }
        User savedUser = userRepository.save(user);
        UserDto dtoResponse = userMapper.toDto(savedUser);
        dtoResponse.setActiveLoansCount((int) loanRepository.countByUserIdAndReturnDateIsNull(id));
        return dtoResponse;
    }

    public void updateApiKey(Long id, String xaiApiKey) {
        if (xaiApiKey == null || xaiApiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("XAI API key cannot be empty");
        }
        // Basic validation
        if (xaiApiKey.length() < 32) {
            throw new IllegalArgumentException("XAI API key must be at least 32 characters");
        }
        User user = userRepository.findById(id).orElseThrow(() -> new LibraryException("User not found: " + id));
        user.setXaiApiKey(xaiApiKey);
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new LibraryException("User not found: " + id);
        }
        long activeCount = loanRepository.countByUserIdAndReturnDateIsNull(id);
        if (activeCount > 0) {
            throw new LibraryException("Cannot delete user because they have " + activeCount + " active loan(s). Please return all books before deleting the user.");
        }
        loanRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }
}
