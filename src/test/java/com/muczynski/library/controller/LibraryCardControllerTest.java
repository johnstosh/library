/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.LibraryCardDesign;
import com.muczynski.library.domain.Authority;
import com.muczynski.library.domain.User;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.LibraryCardPdfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API Integration Tests for LibraryCardController
 *
 * Tests REST endpoints with actual HTTP requests according to backend-development-requirements.md
 * Each endpoint should have:
 * - One test for successful request (2xx status)
 * - One test for unauthorized access (401/403 status)
 * - One test for different designs where applicable
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private LibraryCardPdfService libraryCardPdfService;

    // ==================== GET /api/library-card/print Tests ====================

    @Test
    void testPrintLibraryCard_Success_DefaultDesign() throws Exception {
        // Arrange - User with default design (CLASSICAL_DEVOTION)
        User testUser = createTestUser(1L, "testuser", LibraryCardDesign.CLASSICAL_DEVOTION);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/library-card/print")
                        .with(user("1").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"library-card.pdf\""))
                .andExpect(header().exists("Content-Length"))
                .andReturn();

        // Verify PDF was generated
        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "PDF should have content");

        // Verify PDF signature (starts with %PDF)
        String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertEquals("%PDF", pdfHeader, "PDF should start with %PDF header");
    }

    @Test
    void testPrintLibraryCard_Success_CountrysideYouth() throws Exception {
        // Arrange - User with COUNTRYSIDE_YOUTH design
        User testUser = createTestUser(2L, "user1", LibraryCardDesign.COUNTRYSIDE_YOUTH);
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/library-card/print")
                        .with(user("2").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        // Verify PDF was generated
        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testPrintLibraryCard_Success_SacredHeartPortrait() throws Exception {
        // Arrange - User with SACRED_HEART_PORTRAIT design
        User testUser = createTestUser(3L, "user2", LibraryCardDesign.SACRED_HEART_PORTRAIT);
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/library-card/print")
                        .with(user("3").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void testPrintLibraryCard_Success_RadiantBlessing() throws Exception {
        // Arrange - User with RADIANT_BLESSING design
        User testUser = createTestUser(4L, "user3", LibraryCardDesign.RADIANT_BLESSING);
        when(userRepository.findById(4L))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/library-card/print")
                        .with(user("4").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void testPrintLibraryCard_Success_PatronOfCreatures() throws Exception {
        // Arrange - User with PATRON_OF_CREATURES design
        User testUser = createTestUser(5L, "user4", LibraryCardDesign.PATRON_OF_CREATURES);
        when(userRepository.findById(5L))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/library-card/print")
                        .with(user("5").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void testPrintLibraryCard_Success_ClassicalDevotion() throws Exception {
        // Arrange - User with CLASSICAL_DEVOTION design (default)
        User testUser = createTestUser(6L, "user5", LibraryCardDesign.CLASSICAL_DEVOTION);
        when(userRepository.findById(6L))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/library-card/print")
                        .with(user("6").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void testPrintLibraryCard_Success_AsLibrarian() throws Exception {
        // Arrange - Librarian user can also print cards
        User librarian = createTestUser(7L, "librarian", LibraryCardDesign.CLASSICAL_DEVOTION);
        when(userRepository.findById(7L))
                .thenReturn(Optional.of(librarian));

        // Act & Assert
        mockMvc.perform(get("/api/library-card/print")
                        .with(user("7").authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"library-card.pdf\""));
    }

    @Test
    void testPrintLibraryCard_Unauthorized() throws Exception {
        // Act & Assert - No authentication
        mockMvc.perform(get("/api/library-card/print"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testPrintLibraryCard_UserNotFound() throws Exception {
        // Arrange - User not found in repository
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/library-card/print")
                        .with(user("999").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testPrintLibraryCard_WithNullDesign_UsesDefault() throws Exception {
        // Arrange - User with null design should use default
        User testUser = createTestUser(8L, "testnull", null);
        when(userRepository.findById(8L))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/library-card/print")
                        .with(user("8").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        // Verify PDF was generated with default design
        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 0, "PDF should be generated even with null design");
    }

    @Test
    void testPrintLibraryCard_PDFSize_IsReasonable() throws Exception {
        // Arrange
        User testUser = createTestUser(9L, "sizetest", LibraryCardDesign.CLASSICAL_DEVOTION);
        when(userRepository.findById(9L))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/library-card/print")
                        .with(user("9").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();

        // PDF should be reasonably sized (larger than 10KB, smaller than 5MB)
        assertTrue(pdfBytes.length > 10000, "PDF should be larger than 10KB");
        assertTrue(pdfBytes.length < 5000000, "PDF should be smaller than 5MB");
    }

    @Test
    void testPrintLibraryCard_AllDesigns_GenerateUniquePDFs() throws Exception {
        // Test that different designs produce different PDFs
        LibraryCardDesign[] designs = LibraryCardDesign.values();
        byte[][] pdfResults = new byte[designs.length][];

        for (int i = 0; i < designs.length; i++) {
            Long userId = 10L + i;
            User testUser = createTestUser(userId, "designtest" + i, designs[i]);
            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(testUser));

            MvcResult result = mockMvc.perform(get("/api/library-card/print")
                            .with(user(userId.toString()).authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isOk())
                    .andReturn();

            pdfResults[i] = result.getResponse().getContentAsByteArray();
            assertTrue(pdfResults[i].length > 0, "PDF for design " + designs[i] + " should have content");
        }

        // Verify all PDFs are different (different designs should produce different content)
        for (int i = 0; i < pdfResults.length; i++) {
            for (int j = i + 1; j < pdfResults.length; j++) {
                assertFalse(
                    java.util.Arrays.equals(pdfResults[i], pdfResults[j]),
                    "PDFs for designs " + designs[i] + " and " + designs[j] + " should be different"
                );
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a test user with specified design preference
     */
    private User createTestUser(Long userId, String username, LibraryCardDesign design) {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setPassword("hashedpassword");
        user.setLibraryCardDesign(design);

        // Add authorities
        Set<Authority> authorities = new HashSet<>();
        Authority userAuthority = new Authority();
        userAuthority.setId(1L);
        userAuthority.setName("USER");
        authorities.add(userAuthority);
        user.setAuthorities(authorities);

        return user;
    }
}
