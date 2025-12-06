/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.LibraryCardDesign;
import com.muczynski.library.domain.Role;
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
        User testUser = createTestUser("testuser", LibraryCardDesign.CLASSICAL_DEVOTION);
        when(userRepository.findByUsernameIgnoreCase("testuser"))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/library-card/print")
                        .with(user("testuser").authorities(new SimpleGrantedAuthority("USER"))))
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
        User testUser = createTestUser("user1", LibraryCardDesign.COUNTRYSIDE_YOUTH);
        when(userRepository.findByUsernameIgnoreCase("user1"))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/library-card/print")
                        .with(user("user1").authorities(new SimpleGrantedAuthority("USER"))))
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
        User testUser = createTestUser("user2", LibraryCardDesign.SACRED_HEART_PORTRAIT);
        when(userRepository.findByUsernameIgnoreCase("user2"))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/library-card/print")
                        .with(user("user2").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void testPrintLibraryCard_Success_RadiantBlessing() throws Exception {
        // Arrange - User with RADIANT_BLESSING design
        User testUser = createTestUser("user3", LibraryCardDesign.RADIANT_BLESSING);
        when(userRepository.findByUsernameIgnoreCase("user3"))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/library-card/print")
                        .with(user("user3").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void testPrintLibraryCard_Success_PatronOfCreatures() throws Exception {
        // Arrange - User with PATRON_OF_CREATURES design
        User testUser = createTestUser("user4", LibraryCardDesign.PATRON_OF_CREATURES);
        when(userRepository.findByUsernameIgnoreCase("user4"))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/library-card/print")
                        .with(user("user4").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void testPrintLibraryCard_Success_ClassicalDevotion() throws Exception {
        // Arrange - User with CLASSICAL_DEVOTION design (default)
        User testUser = createTestUser("user5", LibraryCardDesign.CLASSICAL_DEVOTION);
        when(userRepository.findByUsernameIgnoreCase("user5"))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/library-card/print")
                        .with(user("user5").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void testPrintLibraryCard_Success_AsLibrarian() throws Exception {
        // Arrange - Librarian user can also print cards
        User librarian = createTestUser("librarian", LibraryCardDesign.CLASSICAL_DEVOTION);
        when(userRepository.findByUsernameIgnoreCase("librarian"))
                .thenReturn(Optional.of(librarian));

        // Act & Assert
        mockMvc.perform(get("/api/library-card/print")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
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
        when(userRepository.findByUsernameIgnoreCase("nonexistent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/library-card/print")
                        .with(user("nonexistent").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testPrintLibraryCard_WithNullDesign_UsesDefault() throws Exception {
        // Arrange - User with null design should use default
        User testUser = createTestUser("testnull", null);
        when(userRepository.findByUsernameIgnoreCase("testnull"))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/library-card/print")
                        .with(user("testnull").authorities(new SimpleGrantedAuthority("USER"))))
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
        User testUser = createTestUser("sizetest", LibraryCardDesign.CLASSICAL_DEVOTION);
        when(userRepository.findByUsernameIgnoreCase("sizetest"))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/library-card/print")
                        .with(user("sizetest").authorities(new SimpleGrantedAuthority("USER"))))
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
            User testUser = createTestUser("designtest" + i, designs[i]);
            when(userRepository.findByUsernameIgnoreCase("designtest" + i))
                    .thenReturn(Optional.of(testUser));

            MvcResult result = mockMvc.perform(get("/api/library-card/print")
                            .with(user("designtest" + i).authorities(new SimpleGrantedAuthority("USER"))))
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
    private User createTestUser(String username, LibraryCardDesign design) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword("hashedpassword");
        user.setLibraryCardDesign(design);

        // Add roles
        Set<Role> roles = new HashSet<>();
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");
        roles.add(userRole);
        user.setRoles(roles);

        return user;
    }
}
