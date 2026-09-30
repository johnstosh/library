/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.Applied;
import com.muczynski.library.email.ApplicationMailSnapshot;
import com.muczynski.library.email.EmailAddresses;
import com.muczynski.library.email.EmailChangeKinds;
import com.muczynski.library.repository.AppliedRepository;
import com.muczynski.library.util.PasswordHashingUtil;
import com.muczynski.library.util.PhoneNumbers;
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
    private EmailChangeQueue emailChangeQueue;

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
        applied.setPhone(normalizeOptionalPhone(applied.getPhone()));
        if (applied.getStatus() == null) {
            applied.setStatus(Applied.ApplicationStatus.PENDING);
        }
        Applied saved = appliedRepository.save(applied);
        stageApplication(null, ApplicationMailSnapshot.from(saved));
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

    private String normalizeOptionalPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim();
        if (!PhoneNumbers.isValid(trimmed)) {
            throw new LibraryException("Invalid phone number: " + trimmed);
        }
        return trimmed;
    }

    public Applied updateApplied(Long id, Applied applied) {
        Applied existingApplied = appliedRepository.findById(id).orElseThrow(() -> new LibraryException("Applied not found: " + id));
        ApplicationMailSnapshot before = ApplicationMailSnapshot.from(existingApplied);
        if (applied.getStatus() != null) {
            existingApplied.setStatus(applied.getStatus());
        }
        Applied saved = appliedRepository.save(existingApplied);
        stageApplication(before, ApplicationMailSnapshot.from(saved));
        return saved;
    }

    public void deleteApplied(Long id) {
        Applied existing = appliedRepository.findById(id)
                .orElseThrow(() -> new LibraryException("Applied not found: " + id));
        ApplicationMailSnapshot before = ApplicationMailSnapshot.from(existing);
        appliedRepository.delete(existing);
        stageApplication(before, null);
    }

    public void approveApplication(Long id) {
        Applied applied = appliedRepository.findById(id)
                .orElseThrow(() -> new LibraryException("Application not found: " + id));
        ApplicationMailSnapshot before = ApplicationMailSnapshot.from(applied);

        userService.createUserFromApplied(applied);

        applied.setStatus(Applied.ApplicationStatus.APPROVED);
        Applied saved = appliedRepository.save(applied);
        stageApplication(before, ApplicationMailSnapshot.from(saved));
    }

    private void stageApplication(ApplicationMailSnapshot before, ApplicationMailSnapshot after) {
        try {
            if (emailChangeQueue == null) {
                return;
            }
            Long id = after != null ? after.id() : before != null ? before.id() : null;
            if (id == null) {
                return;
            }
            emailChangeQueue.stage("application:" + id, EmailChangeKinds.APPLICATION, before, after);
        } catch (RuntimeException e) {
            logger.warn("Failed to queue application email: {}", e.getMessage());
        }
    }
}
