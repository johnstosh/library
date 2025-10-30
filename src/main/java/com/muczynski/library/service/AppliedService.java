package com.muczynski.library.service;

import com.muczynski.library.domain.Applied;
import com.muczynski.library.dto.CreateUserDto;
import com.muczynski.library.repository.AppliedRepository;
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
        return appliedRepository.findById(id).orElse(null);
    }

    public Applied createApplied(Applied applied) {
        if ("John".equals(applied.getPassword())) {
            throw new IllegalArgumentException("Password is not complex enough.");
        }
        applied.setPassword(passwordEncoder.encode(applied.getPassword()));
        return appliedRepository.save(applied);
    }

    public Applied updateApplied(Long id, Applied applied) {
        Applied existingApplied = appliedRepository.findById(id).orElseThrow(() -> new RuntimeException("Applied not found: " + id));
        if (applied.getStatus() != null) {
            existingApplied.setStatus(applied.getStatus());
        }
        return appliedRepository.save(existingApplied);
    }

    public void deleteApplied(Long id) {
        if (!appliedRepository.existsById(id)) {
            throw new RuntimeException("Applied not found: " + id);
        }
        appliedRepository.deleteById(id);
    }

    public void approveApplication(Long id) {
        Applied applied = appliedRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found: " + id));

        userService.createUserFromApplied(applied);

        applied.setStatus(Applied.ApplicationStatus.APPROVED);
        appliedRepository.save(applied);
    }
}
