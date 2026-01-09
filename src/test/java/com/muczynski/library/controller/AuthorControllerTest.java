/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.service.AuthorService;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.PhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorService authorService;

    @MockitoBean
    private PhotoService photoService;

    @MockitoBean
    private BookService bookService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void createAuthor() throws Exception {
        AuthorDto inputDto = new AuthorDto();
        inputDto.setName("Test Author");
        inputDto.setGrokipediaUrl("https://grokipedia.example.com/author/1");
        AuthorDto returnedDto = new AuthorDto();
        returnedDto.setId(1L);
        returnedDto.setName("Test Author");
        returnedDto.setGrokipediaUrl("https://grokipedia.example.com/author/1");
        when(authorService.createAuthor(any(AuthorDto.class))).thenReturn(returnedDto);

        mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void getAllAuthors() throws Exception {
        AuthorDto dto = new AuthorDto();
        dto.setId(1L);
        dto.setName("Test Author");
        when(authorService.getAllAuthors()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getAuthorById() throws Exception {
        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Test Book");

        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Test Author");
        authorDto.setBooks(Collections.singletonList(bookDto));
        when(authorService.getAuthorById(1L)).thenReturn(authorDto);

        mockMvc.perform(get("/api/authors/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteAuthorPhoto_Success() throws Exception {
        doNothing().when(photoService).deleteAuthorPhoto(1L, 1L);

        mockMvc.perform(delete("/api/authors/1/photos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteAuthorPhoto_NotFound() throws Exception {
        doThrow(new RuntimeException("Photo not found")).when(photoService).deleteAuthorPhoto(1L, 1L);

        mockMvc.perform(delete("/api/authors/1/photos/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getAuthorsWithoutDescription() throws Exception {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Author Without Biography");
        authorDto.setBookCount(5L);
        when(authorService.getAuthorsWithoutDescription()).thenReturn(Collections.singletonList(authorDto));

        mockMvc.perform(get("/api/authors/without-description"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getAuthorsWithZeroBooks() throws Exception {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Author With No Books");
        authorDto.setBookCount(0L);
        when(authorService.getAuthorsWithZeroBooks()).thenReturn(Collections.singletonList(authorDto));

        mockMvc.perform(get("/api/authors/zero-books"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getAuthorsWithoutGrokipedia() throws Exception {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Author Without Grokipedia URL");
        authorDto.setBookCount(3L);
        when(authorService.getAuthorsWithoutGrokipedia()).thenReturn(Collections.singletonList(authorDto));

        mockMvc.perform(get("/api/authors/without-grokipedia"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void updateAuthor() throws Exception {
        AuthorDto inputDto = new AuthorDto();
        inputDto.setName("Updated Author");
        inputDto.setGrokipediaUrl("https://grokipedia.example.com/author/1/updated");
        AuthorDto returnedDto = new AuthorDto();
        returnedDto.setId(1L);
        returnedDto.setName("Updated Author");
        returnedDto.setGrokipediaUrl("https://grokipedia.example.com/author/1/updated");
        when(authorService.updateAuthor(eq(1L), any(AuthorDto.class))).thenReturn(returnedDto);

        mockMvc.perform(put("/api/authors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteAuthor() throws Exception {
        doNothing().when(authorService).deleteAuthor(1L);

        mockMvc.perform(delete("/api/authors/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteAuthor_WithAssociatedBooks_ReturnsConflict() throws Exception {
        doThrow(new RuntimeException("Cannot delete author because it has 3 associated books."))
                .when(authorService).deleteAuthor(1L);

        mockMvc.perform(delete("/api/authors/1"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteBulkAuthors() throws Exception {
        List<Long> authorIds = List.of(1L, 2L, 3L);
        doNothing().when(authorService).deleteBulkAuthors(authorIds);

        mockMvc.perform(post("/api/authors/delete-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorIds)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteAuthorsWithNoBooks() throws Exception {
        when(authorService.deleteAuthorsWithNoBooks()).thenReturn(5);

        mockMvc.perform(post("/api/authors/delete-authors-with-no-books"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void rotatePhotoCW() throws Exception {
        doNothing().when(photoService).rotateAuthorPhoto(1L, 1L, true);

        mockMvc.perform(put("/api/authors/1/photos/1/rotate-cw"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void rotatePhotoCCW() throws Exception {
        doNothing().when(photoService).rotateAuthorPhoto(1L, 1L, false);

        mockMvc.perform(put("/api/authors/1/photos/1/rotate-ccw"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void moveAuthorPhotoLeft() throws Exception {
        doNothing().when(photoService).moveAuthorPhotoLeft(1L, 1L);

        mockMvc.perform(put("/api/authors/1/photos/1/move-left"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void moveAuthorPhotoRight() throws Exception {
        doNothing().when(photoService).moveAuthorPhotoRight(1L, 1L);

        mockMvc.perform(put("/api/authors/1/photos/1/move-right"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getPhotosByAuthorId() throws Exception {
        PhotoDto photoDto = new PhotoDto();
        photoDto.setId(1L);
        when(photoService.getPhotosByAuthorId(1L)).thenReturn(Collections.singletonList(photoDto));

        mockMvc.perform(get("/api/authors/1/photos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getBooksByAuthorId() throws Exception {
        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Test Book");
        when(bookService.getBooksByAuthorId(1L)).thenReturn(Collections.singletonList(bookDto));

        mockMvc.perform(get("/api/authors/1/books"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void addPhotoToAuthor() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        PhotoDto photoDto = new PhotoDto();
        photoDto.setId(1L);
        when(photoService.addPhotoToAuthor(eq(1L), any())).thenReturn(photoDto);

        mockMvc.perform(multipart("/api/authors/1/photos")
                        .file(file))
                .andExpect(status().isCreated());
    }

    // Tests for unauthenticated access (no @WithMockUser annotation)

    @Test
    void getAuthorById_unauthenticated_returnsOk() throws Exception {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Test Author");
        when(authorService.getAuthorById(1L)).thenReturn(authorDto);

        mockMvc.perform(get("/api/authors/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllAuthors_unauthenticated_returnsOk() throws Exception {
        AuthorDto dto = new AuthorDto();
        dto.setId(1L);
        dto.setName("Test Author");
        when(authorService.getAllAuthors()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk());
    }

    @Test
    void getPhotosByAuthorId_unauthenticated_returnsOk() throws Exception {
        PhotoDto photoDto = new PhotoDto();
        photoDto.setId(1L);
        when(photoService.getPhotosByAuthorId(1L)).thenReturn(Collections.singletonList(photoDto));

        mockMvc.perform(get("/api/authors/1/photos"))
                .andExpect(status().isOk());
    }

    @Test
    void getBooksByAuthorId_unauthenticated_returnsOk() throws Exception {
        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Test Book");
        when(bookService.getBooksByAuthorId(1L)).thenReturn(Collections.singletonList(bookDto));

        mockMvc.perform(get("/api/authors/1/books"))
                .andExpect(status().isOk());
    }

    @Test
    void getAuthorsWithoutDescription_unauthenticated_returnsOk() throws Exception {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Author Without Biography");
        when(authorService.getAuthorsWithoutDescription()).thenReturn(Collections.singletonList(authorDto));

        mockMvc.perform(get("/api/authors/without-description"))
                .andExpect(status().isOk());
    }

    @Test
    void getAuthorsWithZeroBooks_unauthenticated_returnsOk() throws Exception {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Author With No Books");
        when(authorService.getAuthorsWithZeroBooks()).thenReturn(Collections.singletonList(authorDto));

        mockMvc.perform(get("/api/authors/zero-books"))
                .andExpect(status().isOk());
    }

    @Test
    void getAuthorsFromMostRecentDay_unauthenticated_returnsOk() throws Exception {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Recent Author");
        when(authorService.getAuthorsFromMostRecentDay()).thenReturn(Collections.singletonList(authorDto));

        mockMvc.perform(get("/api/authors/most-recent-day"))
                .andExpect(status().isOk());
    }

    @Test
    void getAuthorsWithoutGrokipedia_unauthenticated_returnsOk() throws Exception {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Author Without Grokipedia");
        when(authorService.getAuthorsWithoutGrokipedia()).thenReturn(Collections.singletonList(authorDto));

        mockMvc.perform(get("/api/authors/without-grokipedia"))
                .andExpect(status().isOk());
    }
}
