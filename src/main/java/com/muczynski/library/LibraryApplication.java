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
                Role librarianRole = roleRepository.findByName("LIBRARIAN").orElseGet(() -> {
                    Role role = new Role();
                    role.setName("LIBRARIAN");
                    return roleRepository.save(role);
                });

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
