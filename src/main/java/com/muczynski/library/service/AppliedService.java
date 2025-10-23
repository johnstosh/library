package com.muczynski.library.service;

import com.muczynski.library.domain.Applied;
import com.muczynski.library.domain.Role;
import com.muczynski.library.domain.User;
import com.muczynski.library.repository.AppliedRepository;
import com.muczynski.library.repository.RoleRepository;
import com.muczynski.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class AppliedService {

    @Autowired
    private AppliedRepository appliedRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Applied> getAllApplied() {
        return appliedRepository.findAll();
    }

    public Applied getAppliedById(Long id) {
        return appliedRepository.findById(id).orElse(null);
    }

    public Applied createApplied(Applied applied) {
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

        userRepository.findByUsername(applied.getName()).ifPresent(u -> {
            throw new RuntimeException("User already exists: " + applied.getName());
        });

        User user = new User();
        user.setUsername(applied.getName());
        user.setPassword(applied.getPassword());

        Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("USER");
            return roleRepository.save(newRole);
        });
        user.setRoles(Collections.singleton(userRole));
        userRepository.save(user);

        applied.setStatus(Applied.ApplicationStatus.APPROVED);
        appliedRepository.save(applied);
    }
}