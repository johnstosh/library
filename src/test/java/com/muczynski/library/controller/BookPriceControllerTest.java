/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.dto.BookPriceDto;
import com.muczynski.library.dto.BookPriceLookupResultDto;
import com.muczynski.library.dto.BookSummaryDto;
import com.muczynski.library.service.BookPriceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void getAllPriceSummaries_librarian_returnsSummaries() throws Exception {
        BookSummaryDto summary1 = new BookSummaryDto();
        summary1.setId(1L);
        summary1.setLastModified(LocalDateTime.of(2025, 1, 1, 12, 0));

        BookSummaryDto summary2 = new BookSummaryDto();
        summary2.setId(2L);
        summary2.setLastModified(LocalDateTime.of(2025, 1, 2, 12, 0));

        when(bookPriceService.getAllPriceSummaries()).thenReturn(Arrays.asList(summary1, summary2));

        String response = mockMvc.perform(get("/api/prices/summaries"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assert response.contains("\"lastModified\":\"2025-01-01T12:00:00\"")
            : "Expected lastModified to be serialized as ISO string, but got: " + response;
        assert !response.contains("\"lastModified\":[2025")
            : "lastModified should not be serialized as array: " + response;
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void getPricesByIds_librarian_returnsPrices() throws Exception {
        BookPriceDto price1 = BookPriceDto.builder()
                .id(1L)
                .bookId(10L)
                .bookTitle("Test Book")
                .cover(BookCoverType.HARDCOVER)
                .priceDollars(new BigDecimal("5.00"))
                .build();

        List<Long> ids = Arrays.asList(1L);
        when(bookPriceService.getPricesByIds(ids)).thenReturn(Arrays.asList(price1));

        mockMvc.perform(post("/api/prices/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookTitle").value("Test Book"));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void getPricesByIdsEmptyList_returnsEmpty() throws Exception {
        when(bookPriceService.getPricesByIds(Collections.emptyList())).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/api/prices/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.emptyList())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void summariesAndByIds_regularUser_forbidden() throws Exception {
        mockMvc.perform(get("/api/prices/summaries"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/prices/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden());
    }
}
