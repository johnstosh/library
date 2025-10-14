package com.muczynski.library.controller;

import com.muczynski.library.dto.CreateUserDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.notFound().build();
        }
        UserDto userDto = new UserDto();
        userDto.setUsername(userDetails.getUsername());
        userDto.setRoles(userDetails.getAuthorities().stream().map(auth -> auth.getAuthority()).collect(Collectors.toSet()));
        return ResponseEntity.ok(userDto);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);
        return user != null ? new ResponseEntity<>(user, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserDto createUserDto) {
        UserDto createdUser = userService.createUser(createUserDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
}
