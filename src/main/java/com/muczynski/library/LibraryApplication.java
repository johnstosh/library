/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library;

import com.muczynski.library.domain.Authority;
import com.muczynski.library.domain.User;
import com.muczynski.library.repository.AuthorityRepository;
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
    public CommandLineRunner initData(UserRepository userRepository, AuthorityRepository authorityRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if the default "librarian" user already exists
            List<User> existingLibrarians = userRepository.findAllByUsernameWithPasswordOrderByIdAsc("librarian");

            if (existingLibrarians.isEmpty()) {
                // Create LIBRARIAN authority if it doesn't exist
                List<Authority> existingAuthorities = authorityRepository.findAllByNameOrderByIdAsc("LIBRARIAN");
                Authority librarianAuthority;
                if (existingAuthorities.isEmpty()) {
                    Authority authority = new Authority();
                    authority.setName("LIBRARIAN");
                    librarianAuthority = authorityRepository.save(authority);
                } else {
                    librarianAuthority = existingAuthorities.get(0); // Select the one with the lowest ID
                }

                // Create the default librarian user
                User librarianUser = new User();
                librarianUser.setUsername("librarian");
                // Hash with SHA-256 first (matching frontend), then BCrypt encode
                String sha256Hash = PasswordHashingUtil.hashPasswordSHA256("divinemercy");
                librarianUser.setPassword(passwordEncoder.encode(sha256Hash));
                librarianUser.setAuthorities(Set.of(librarianAuthority));
                userRepository.save(librarianUser);
            }
        };
    }

}
