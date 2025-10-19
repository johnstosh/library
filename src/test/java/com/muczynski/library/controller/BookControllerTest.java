package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.PhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
        BookDto returnedDto = new BookDto();
        returnedDto.setId(1L);
        returnedDto.setTitle("Test Book");
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
        when(bookService.getBookById(1L)).thenReturn(bookDto);

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void updateBook() throws Exception {
        BookDto inputDto = new BookDto();
        inputDto.setTitle("Updated Book");
        BookDto returnedDto = new BookDto();
        returnedDto.setId(1L);
        returnedDto.setTitle("Updated Book");
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
    void bulkImportBooks() throws Exception {
        BookDto dto = new BookDto();
        dto.setTitle("Test Book Bulk");
        dto.setAuthorId(1L);
        dto.setLibraryId(1L);
        List<BookDto> inputDtos = Collections.singletonList(dto);
        doNothing().when(bookService).bulkImportBooks(any());

        mockMvc.perform(post("/api/books/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDtos)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void addPhotoToBook() throws Exception {
        PhotoDto inputDto = new PhotoDto();
        inputDto.setBase64("dGVzdFBob3Rv");
        PhotoDto returnedDto = new PhotoDto();
        returnedDto.setId(1L);
        returnedDto.setBase64("dGVzdFBob3Rv");
        when(photoService.addPhoto(eq(1L), any(PhotoDto.class))).thenReturn(returnedDto);

        mockMvc.perform(post("/api/books/1/photos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated());
    }
}
