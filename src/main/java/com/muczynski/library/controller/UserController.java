package com.muczynski.library.controller;

import com.muczynski.library.dto.CreateUserDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto userDto = new UserDto();
        userDto.setUsername(userDetails.getUsername());
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());
        userDto.setRoles(roles);
        // Load full user details including ID and API key by username
        UserDto fullUser = userService.getUserByUsername(userDetails.getUsername());
        if (fullUser != null) {
            userDto.setId(fullUser.getId());
            userDto.setXaiApiKey(fullUser.getXaiApiKey());
        }
        return ResponseEntity.ok(userDto);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<UserDto> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UserDto user = userService.getUserById(id);
            return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> createUser(@RequestBody CreateUserDto createUserDto) {
        try {
            UserDto createdUser = userService.createUser(createUserDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/public/register")
    public ResponseEntity<?> registerUser(@RequestBody CreateUserDto createUserDto) {
        try {
            if (!"USER".equals(createUserDto.getRole())) {
                return ResponseEntity.badRequest().build();
            }
            UserDto createdUser = userService.createUser(createUserDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody CreateUserDto createUserDto) {
        try {
            UserDto updatedUser = userService.updateUser(id, createUserDto);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/apikey")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updateApiKey(@PathVariable Long id, @RequestBody UserDto userDto) {
        try {
            userService.updateApiKey(id, userDto.getXaiApiKey());
            UserDto updatedUser = userService.getUserById(id);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("active loan")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", e.getMessage()));
            }
            throw e;
        }
    }
}
