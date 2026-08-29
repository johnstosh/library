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
import java.util.Optional;

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

    @Test
    void getAllApplied_returnsOnlyApplicationsAwaitingReview() {
        Applied pending = new Applied();
        pending.setId(1L);
        pending.setStatus(Applied.ApplicationStatus.PENDING);
        Applied approved = new Applied();
        approved.setId(2L);
        approved.setStatus(Applied.ApplicationStatus.APPROVED);
        Applied question = new Applied();
        question.setId(3L);
        question.setStatus(Applied.ApplicationStatus.QUESTION);
        Applied notApproved = new Applied();
        notApproved.setId(4L);
        notApproved.setStatus(Applied.ApplicationStatus.NOT_APPROVED);
        Applied unset = new Applied();
        unset.setId(5L);
        unset.setStatus(null);
        when(appliedRepository.findAll()).thenReturn(List.of(pending, approved, question, notApproved, unset));

        List<Applied> open = appliedService.getAllApplied();

        assertEquals(List.of(1L, 3L, 5L), open.stream().map(Applied::getId).toList());
    }

    @Test
    void approveApplication_createsUserAndMarksApproved() {
        Applied applied = new Applied();
        applied.setId(1L);
        applied.setName("Jane Doe");
        applied.setStatus(Applied.ApplicationStatus.PENDING);
        when(appliedRepository.findById(1L)).thenReturn(Optional.of(applied));

        appliedService.approveApplication(1L);

        verify(userService).createUserFromApplied(applied);
        ArgumentCaptor<Applied> captor = ArgumentCaptor.forClass(Applied.class);
        verify(appliedRepository).save(captor.capture());
        assertEquals(Applied.ApplicationStatus.APPROVED, captor.getValue().getStatus());
    }

    @Test
    void approveApplication_duplicateUsernameIsReported() {
        Applied applied = new Applied();
        applied.setId(1L);
        applied.setName("Jane Doe");
        when(appliedRepository.findById(1L)).thenReturn(Optional.of(applied));
        when(userService.createUserFromApplied(applied))
                .thenThrow(new LibraryException("A user named 'Jane Doe' already exists"));

        LibraryException ex = assertThrows(LibraryException.class,
                () -> appliedService.approveApplication(1L));
        assertEquals("A user named 'Jane Doe' already exists", ex.getMessage());
        verify(appliedRepository, never()).save(any());
    }

    @Test
    void approveApplication_missingApplicationThrows() {
        when(appliedRepository.findById(99L)).thenReturn(Optional.empty());

        LibraryException ex = assertThrows(LibraryException.class,
                () -> appliedService.approveApplication(99L));
        assertEquals("Application not found: 99", ex.getMessage());
        verify(userService, never()).createUserFromApplied(any());
    }
}
