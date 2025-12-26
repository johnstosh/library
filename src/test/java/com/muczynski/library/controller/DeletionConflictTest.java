/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.security.MessageDigest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeletionConflictTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Helper method to hash passwords with SHA-256 (matching frontend behavior)
     */
    private String hashPassword(String plainPassword) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(plainPassword.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteAuthorWithAssociatedBooksReturns409() throws Exception {
        // Create library
        MvcResult libraryResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "St. Martin de Porres",
                                    "hostname": "library.muczynskifamily.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long libraryId = Long.valueOf(libraryResult.getResponse().getContentAsString().split("\"id\":")[1].split(",")[0]);

        // Create author
        MvcResult authorResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Test Author"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long authorId = Long.valueOf(authorResult.getResponse().getContentAsString().split("\"id\":")[1].split(",")[0]);

        // Create book associated with author
        mockMvc.perform(MockMvcRequestBuilders.post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Test Book",
                                    "authorId": %d,
                                    "libraryId": %d
                                }
                                """.formatted(authorId, libraryId)))
                .andExpect(status().isCreated());

        // Attempt to delete author - should conflict
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/authors/{id}", authorId))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteBookWithAssociatedLoansReturns409() throws Exception {
        // Create library
        MvcResult libraryResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "St. Martin de Porres",
                                    "hostname": "library.muczynskifamily.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long libraryId = Long.valueOf(libraryResult.getResponse().getContentAsString().split("\"id\":")[1].split(",")[0]);

        // Create author
        MvcResult authorResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Test Author"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long authorId = Long.valueOf(authorResult.getResponse().getContentAsString().split("\"id\":")[1].split(",")[0]);

        // Create book
        MvcResult bookResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Test Book",
                                    "authorId": %d,
                                    "libraryId": %d
                                }
                                """.formatted(authorId, libraryId)))
                .andExpect(status().isCreated())
                .andReturn();
        Long bookId = Long.valueOf(bookResult.getResponse().getContentAsString().split("\"id\":")[1].split(",")[0]);

        // Create user with unique username
        String hashedPassword = hashPassword("password");
        MvcResult userResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "integration_test_user",
                                    "password": "%s",
                                    "authority": "USER"
                                }
                                """.formatted(hashedPassword)))
                .andExpect(status().isCreated())
                .andReturn();
        Long userId = Long.valueOf(userResult.getResponse().getContentAsString().split("\"id\":")[1].split(",")[0]);

        // Create loan for the book
        mockMvc.perform(MockMvcRequestBuilders.post("/api/loans/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "bookId": %d,
                                    "userId": %d
                                }
                                """.formatted(bookId, userId)))
                .andExpect(status().isCreated());

        // Attempt to delete book - should conflict
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/books/{id}", bookId))
                .andExpect(status().isConflict());
    }
}
