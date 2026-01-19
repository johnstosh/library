/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.Applied;
import com.muczynski.library.dto.CreateUserDto;
import com.muczynski.library.repository.AppliedRepository;
import com.muczynski.library.util.PasswordHashingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AppliedService {

    @Autowired
    private AppliedRepository appliedRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    public List<Applied> getAllApplied() {
        return appliedRepository.findAll();
    }

    public Applied getAppliedById(Long id) {
        return appliedRepository.findById(id)
                .orElseThrow(() -> new LibraryException("Applied not found: " + id));
    }

    public Applied createApplied(Applied applied) {
        // Validate password is SHA-256 hash from frontend
        if (!PasswordHashingUtil.isValidSHA256Hash(applied.getPassword())) {
            throw new IllegalArgumentException("Invalid password format - expected SHA-256 hash");
        }
        applied.setPassword(passwordEncoder.encode(applied.getPassword()));
        return appliedRepository.save(applied);
    }

    public Applied updateApplied(Long id, Applied applied) {
        Applied existingApplied = appliedRepository.findById(id).orElseThrow(() -> new LibraryException("Applied not found: " + id));
        if (applied.getStatus() != null) {
            existingApplied.setStatus(applied.getStatus());
        }
        return appliedRepository.save(existingApplied);
    }

    public void deleteApplied(Long id) {
        if (!appliedRepository.existsById(id)) {
            throw new LibraryException("Applied not found: " + id);
        }
        appliedRepository.deleteById(id);
    }

    public void approveApplication(Long id) {
        Applied applied = appliedRepository.findById(id)
                .orElseThrow(() -> new LibraryException("Application not found: " + id));

        userService.createUserFromApplied(applied);

        applied.setStatus(Applied.ApplicationStatus.APPROVED);
        appliedRepository.save(applied);
    }
}
