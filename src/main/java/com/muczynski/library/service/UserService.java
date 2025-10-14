package com.muczynski.library.service;

import com.muczynski.library.domain.Role;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.CreateUserDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.mapper.UserMapper;
import com.muczynski.library.repository.RoleRepository;
import com.muczynski.library.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElse(null);
    }

    public UserDto createUser(CreateUserDto dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        Role role = roleRepository.findByName(dto.getRole()).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(dto.getRole());
            return roleRepository.save(newRole);
        });
        user.setRoles(Collections.singleton(role));

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }
}
