/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.BookSummaryDto;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.PhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private PhotoService photoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void createBook() throws Exception {
        BookDto inputDto = new BookDto();
        inputDto.setTitle("Test Book");
        inputDto.setAuthorId(1L);
        inputDto.setLibraryId(1L);
        inputDto.setLocNumber(null);
        inputDto.setStatusReason(null);
        BookDto returnedDto = new BookDto();
        returnedDto.setId(1L);
        returnedDto.setTitle("Test Book");
        returnedDto.setLocNumber(null);
        returnedDto.setStatusReason(null);
        when(bookService.createBook(any(BookDto.class))).thenReturn(returnedDto);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void getAllBooks() throws Exception {
        BookDto dto = new BookDto();
        dto.setId(1L);
        dto.setTitle("Test Book");
        dto.setLocNumber(null);
        dto.setStatusReason(null);
        when(bookService.getAllBooks()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getBookById() throws Exception {
        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Test Book");
        bookDto.setLocNumber(null);
        bookDto.setStatusReason(null);
        when(bookService.getBookById(1L)).thenReturn(bookDto);

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void updateBook() throws Exception {
        BookDto inputDto = new BookDto();
        inputDto.setTitle("Updated Book");
        inputDto.setLocNumber("Test LOC");
        inputDto.setStatusReason("Test reason");
        BookDto returnedDto = new BookDto();
        returnedDto.setId(1L);
        returnedDto.setTitle("Updated Book");
        returnedDto.setLocNumber("Test LOC");
        returnedDto.setStatusReason("Test reason");
        when(bookService.updateBook(eq(1L), any(BookDto.class))).thenReturn(returnedDto);

        mockMvc.perform(put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteBook() throws Exception {
        doNothing().when(bookService).deleteBook(1L);

        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void addPhotoToBook() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test image".getBytes());
        PhotoDto returnedDto = new PhotoDto();
        returnedDto.setId(1L);
        when(photoService.addPhoto(eq(1L), any(MultipartFile.class))).thenReturn(returnedDto);

        mockMvc.perform(multipart("/api/books/1/photos")
                        .file(file))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void rotatePhotoCW() throws Exception {
        doNothing().when(photoService).rotatePhoto(eq(1L), eq(true));

        mockMvc.perform(put("/api/books/1/photos/1/rotate-cw"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void rotatePhotoCCW() throws Exception {
        doNothing().when(photoService).rotatePhoto(eq(1L), eq(false));

        mockMvc.perform(put("/api/books/1/photos/1/rotate-ccw"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getAllBookSummaries() throws Exception {
        BookSummaryDto summary1 = new BookSummaryDto();
        summary1.setId(1L);
        summary1.setLastModified(LocalDateTime.of(2025, 1, 1, 12, 0));

        BookSummaryDto summary2 = new BookSummaryDto();
        summary2.setId(2L);
        summary2.setLastModified(LocalDateTime.of(2025, 1, 2, 12, 0));

        when(bookService.getAllBookSummaries()).thenReturn(Arrays.asList(summary1, summary2));

        mockMvc.perform(get("/api/books/summaries"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getBooksByIds() throws Exception {
        BookDto book1 = new BookDto();
        book1.setId(1L);
        book1.setTitle("Test Book 1");

        BookDto book2 = new BookDto();
        book2.setId(2L);
        book2.setTitle("Test Book 2");

        List<Long> ids = Arrays.asList(1L, 2L);
        when(bookService.getBooksByIds(any(List.class))).thenReturn(Arrays.asList(book1, book2));

        mockMvc.perform(post("/api/books/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getBooksByIdsEmptyList() throws Exception {
        when(bookService.getBooksByIds(any(List.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/api/books/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.emptyList())))
                .andExpect(status().isOk());
    }
}
