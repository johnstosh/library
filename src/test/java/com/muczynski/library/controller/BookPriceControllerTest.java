/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.dto.BookPriceDto;
import com.muczynski.library.dto.BookPriceLookupResultDto;
import com.muczynski.library.service.BookPriceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookPriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookPriceService bookPriceService;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void listPrices_librarian_returnsRows() throws Exception {
        when(bookPriceService.listAll()).thenReturn(List.of(
                BookPriceDto.builder()
                        .id(9L)
                        .bookId(1L)
                        .bookTitle("Pride and Prejudice")
                        .cover(BookCoverType.HARDCOVER)
                        .priceDollars(new BigDecimal("4.86"))
                        .shippingDollars(BigDecimal.ZERO)
                        .totalDollars(new BigDecimal("4.86"))
                        .build()
        ));

        mockMvc.perform(get("/api/prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookTitle").value("Pride and Prejudice"))
                .andExpect(jsonPath("$[0].cover").value("HARDCOVER"))
                .andExpect(jsonPath("$[0].priceDollars").value(4.86));
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void listPrices_regularUser_forbidden() throws Exception {
        mockMvc.perform(get("/api/prices"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listPrices_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/api/prices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void lookupBook_librarian_returnsResult() throws Exception {
        when(bookPriceService.lookupAndUpdateBook(eq(1L))).thenReturn(
                BookPriceLookupResultDto.builder()
                        .bookId(1L)
                        .bookTitle("Pride and Prejudice")
                        .success(true)
                        .hardcover(BookPriceDto.builder()
                                .bookId(1L)
                                .cover(BookCoverType.HARDCOVER)
                                .priceDollars(new BigDecimal("4.86"))
                                .build())
                        .build()
        );

        mockMvc.perform(post("/api/prices/lookup/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.hardcover.priceDollars").value(4.86));
    }
}
