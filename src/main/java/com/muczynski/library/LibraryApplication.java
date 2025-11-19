/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library;

import com.muczynski.library.domain.Role;
import com.muczynski.library.domain.User;
import com.muczynski.library.repository.RoleRepository;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.util.PasswordHashingUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

@SpringBootApplication
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                // Use list-based query to handle potential duplicates gracefully
                List<Role> existingRoles = roleRepository.findAllByNameOrderByIdAsc("LIBRARIAN");
                Role librarianRole;
                if (existingRoles.isEmpty()) {
                    Role role = new Role();
                    role.setName("LIBRARIAN");
                    librarianRole = roleRepository.save(role);
                } else {
                    librarianRole = existingRoles.get(0); // Select the one with the lowest ID
                }

                User librarianUser = new User();
                librarianUser.setUsername("librarian");
                // Hash with SHA-256 first (matching frontend), then BCrypt encode
                String sha256Hash = PasswordHashingUtil.hashPasswordSHA256("divinemercy");
                librarianUser.setPassword(passwordEncoder.encode(sha256Hash));
                librarianUser.setRoles(Set.of(librarianRole));
                userRepository.save(librarianUser);
            }
        };
    }

}
