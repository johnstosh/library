/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Direct loads of React routes must serve index.html. Missing SpaController
 * mappings surface as INTERNAL_ERROR "No static resource …".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void spaPricesPage_librarian_servesIndexHtml() throws Exception {
        mockMvc.perform(get("/prices"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void spaPricesPage_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/prices"))
                .andExpect(status().isUnauthorized());
    }
}
