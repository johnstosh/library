/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.Applied;
import com.muczynski.library.email.EmailAddresses;
import com.muczynski.library.email.PendingApplicationNotice;
import com.muczynski.library.repository.AppliedRepository;
import com.muczynski.library.util.PasswordHashingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AppliedService {

    private static final Logger logger = LoggerFactory.getLogger(AppliedService.class);

    @Autowired
    private AppliedRepository appliedRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationEmailService applicationEmailService;

    public List<Applied> getAllApplied() {
        return appliedRepository.findAll();
    }

    public Applied getAppliedById(Long id) {
        return appliedRepository.findById(id)
                .orElseThrow(() -> new LibraryException("Applied not found: " + id));
    }

    public Applied createApplied(Applied applied) {
        // Check for duplicate application by name
        List<Applied> existing = appliedRepository.findAllByNameOrderByIdAsc(applied.getName());
        if (!existing.isEmpty()) {
            throw new LibraryException("An application already exists for '" + applied.getName() + "'");
        }

        // Validate password is SHA-256 hash from frontend
        if (!PasswordHashingUtil.isValidSHA256Hash(applied.getPassword())) {
            throw new IllegalArgumentException("Invalid password format - expected SHA-256 hash");
        }
        applied.setPassword(passwordEncoder.encode(applied.getPassword()));
        applied.setEmail(normalizeOptionalEmail(applied.getEmail()));
        if (applied.getStatus() == null) {
            applied.setStatus(Applied.ApplicationStatus.PENDING);
        }
        Applied saved = appliedRepository.save(applied);
        if (saved.getStatus() == Applied.ApplicationStatus.PENDING) {
            try {
                applicationEmailService.notifyPendingApplication(
                        new PendingApplicationNotice(saved.getId(), saved.getName(), saved.getEmail()));
            } catch (Exception e) {
                logger.warn("Failed to dispatch pending-application email for '{}': {}",
                        saved.getName(), e.getMessage());
            }
        }
        return saved;
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String trimmed = email.trim();
        if (!EmailAddresses.isValid(trimmed)) {
            throw new LibraryException("Invalid email address: " + trimmed);
        }
        return trimmed;
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
