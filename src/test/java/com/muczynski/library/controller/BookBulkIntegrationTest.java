package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.domain.BookStatus;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookBulkIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final Random random = new Random();

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void createAndDelete100RandomBooks() throws Exception {
        // Step 0: Get initial book count (should be 1 from data.sql)
        MvcResult initialResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/books"))
                .andExpect(status().isOk())
                .andReturn();
        List<BookDto> initialBooks = objectMapper.readValue(initialResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, BookDto.class));
        int initialCount = initialBooks.size();
        assertEquals(1, initialCount, "Expected 1 initial book from data.sql");

        // Step 1: Create 100 random books via bulk import
        List<BookDto> bookDtos = new ArrayList<>();
        List<String> createdTitles = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            BookDto dto = new BookDto();
            String uniqueTitle = "Random Book " + UUID.randomUUID().toString().substring(0, 8);
            createdTitles.add(uniqueTitle);
            dto.setTitle(uniqueTitle);
            dto.setPublicationYear(2000 + random.nextInt(24)); // 2000-2023
            dto.setPublisher("Random Publisher " + (random.nextInt(10) + 1));
            dto.setPlotSummary("Random plot summary for book " + i);
            dto.setRelatedWorks("Related work " + i);
            dto.setDetailedDescription("Detailed random description " + i);
            dto.setStatus(BookStatus.values()[random.nextInt(BookStatus.values().length)]);
            dto.setAuthorId(1L); // Use existing author from data.sql
            dto.setLibraryId(1L); // Use existing library from data.sql
            bookDtos.add(dto);
        }

        mockMvc.perform(MockMvcRequestBuilders.post("/api/books/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookDtos)))
                .andExpect(status().isOk());

        // Step 2: Verify creation by fetching all books and checking exact count
        MvcResult afterCreateResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/books"))
                .andExpect(status().isOk())
                .andReturn();
        List<BookDto> afterCreateBooks = objectMapper.readValue(afterCreateResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, BookDto.class));
        assertEquals(initialCount + 100, afterCreateBooks.size(), "Expected " + (initialCount + 100) + " books after bulk creation");

        // Collect IDs of created books (those with titles starting with "Random Book")
        List<Long> createdBookIds = afterCreateBooks.stream()
                .filter(book -> book.getTitle().startsWith("Random Book"))
                .map(BookDto::getId)
                .collect(Collectors.toList());
        assertEquals(100, createdBookIds.size(), "Expected 100 created books identifiable by title");

        // Step 3: Delete the 100 created books using individual DELETE endpoints
        for (Long bookId : createdBookIds) {
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/books/" + bookId))
                    .andExpect(status().isNoContent());
        }

        // Step 4: Verify deletion by fetching all books and checking count back to initial
        MvcResult finalResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/books"))
                .andExpect(status().isOk())
                .andReturn();
        List<BookDto> finalBooks = objectMapper.readValue(finalResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, BookDto.class));
        assertEquals(initialCount, finalBooks.size(), "Expected " + initialCount + " books after deleting 100 created ones");

        // Additional verification: No remaining "Random Book" titles
        long remainingRandomBooks = finalBooks.stream()
                .filter(book -> book.getTitle().startsWith("Random Book"))
                .count();
        assertEquals(0, remainingRandomBooks, "Expected no remaining random books after deletion");
    }
}
