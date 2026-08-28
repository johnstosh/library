/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Applied;
import com.muczynski.library.email.PendingApplicationNotice;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.AppliedRepository;
import com.muczynski.library.util.PasswordHashingUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppliedServiceTest {

    private static final String SHA256 = PasswordHashingUtil.hashPasswordSHA256("password123");

    @Mock
    private AppliedRepository appliedRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEmailService applicationEmailService;

    @InjectMocks
    private AppliedService appliedService;

    @Test
    void createApplied_notifiesWhenPending() {
        Applied incoming = new Applied();
        incoming.setName("Jane Doe");
        incoming.setEmail("jane@example.com");
        incoming.setPassword(SHA256);

        when(appliedRepository.findAllByNameOrderByIdAsc("Jane Doe")).thenReturn(List.of());
        when(passwordEncoder.encode(SHA256)).thenReturn("$2a$hashed");
        when(appliedRepository.save(any(Applied.class))).thenAnswer(invocation -> {
            Applied saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        Applied result = appliedService.createApplied(incoming);

        assertEquals(42L, result.getId());
        assertEquals("jane@example.com", result.getEmail());
        assertEquals(Applied.ApplicationStatus.PENDING, result.getStatus());

        ArgumentCaptor<PendingApplicationNotice> captor = ArgumentCaptor.forClass(PendingApplicationNotice.class);
        verify(applicationEmailService).notifyPendingApplication(captor.capture());
        assertEquals(42L, captor.getValue().getApplicationId());
        assertEquals("Jane Doe", captor.getValue().getApplicantName());
        assertEquals("jane@example.com", captor.getValue().getApplicantEmail());
    }

    @Test
    void createApplied_blankEmailStoredAsNull() {
        Applied incoming = new Applied();
        incoming.setName("Jane Doe");
        incoming.setEmail("  ");
        incoming.setPassword(SHA256);

        when(appliedRepository.findAllByNameOrderByIdAsc("Jane Doe")).thenReturn(List.of());
        when(passwordEncoder.encode(SHA256)).thenReturn("$2a$hashed");
        when(appliedRepository.save(any(Applied.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Applied result = appliedService.createApplied(incoming);
        assertNull(result.getEmail());
    }

    @Test
    void createApplied_invalidEmailRejected() {
        Applied incoming = new Applied();
        incoming.setName("Jane Doe");
        incoming.setEmail("not-an-email");
        incoming.setPassword(SHA256);

        when(appliedRepository.findAllByNameOrderByIdAsc("Jane Doe")).thenReturn(List.of());

        assertThrows(LibraryException.class, () -> appliedService.createApplied(incoming));
        verify(appliedRepository, never()).save(any());
        verify(applicationEmailService, never()).notifyPendingApplication(any());
    }

    @Test
    void createApplied_emailFailureDoesNotFailSave() {
        Applied incoming = new Applied();
        incoming.setName("Jane Doe");
        incoming.setPassword(SHA256);

        when(appliedRepository.findAllByNameOrderByIdAsc("Jane Doe")).thenReturn(List.of());
        when(passwordEncoder.encode(SHA256)).thenReturn("$2a$hashed");
        when(appliedRepository.save(any(Applied.class))).thenAnswer(invocation -> {
            Applied saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        doThrow(new RuntimeException("mail down")).when(applicationEmailService).notifyPendingApplication(any());

        Applied result = appliedService.createApplied(incoming);
        assertEquals(1L, result.getId());
    }

    @Test
    void createApplied_duplicateDoesNotEmail() {
        Applied incoming = new Applied();
        incoming.setName("Jane Doe");
        incoming.setPassword(SHA256);

        when(appliedRepository.findAllByNameOrderByIdAsc("Jane Doe")).thenReturn(List.of(new Applied()));

        assertThrows(LibraryException.class, () -> appliedService.createApplied(incoming));
        verify(applicationEmailService, never()).notifyPendingApplication(any());
    }
}
