/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.BookSummaryDto;
import com.muczynski.library.dto.SavedBookDto;
import com.muczynski.library.dto.GrokipediaLookupResultDto;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.service.AskGrok;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.GrokipediaLookupService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    @MockitoBean
    private AskGrok askGrok;

    @MockitoBean
    private GrokipediaLookupService grokipediaLookupService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void createBook() throws Exception {
        BookDto inputDto = new BookDto();
        inputDto.setTitle("Test Book");
        inputDto.setAuthorId(1L);
        inputDto.setLibraryId(1L);
        inputDto.setGrokipediaUrl("https://grokipedia.example.com/book/1");
        inputDto.setFreeTextUrl("https://www.gutenberg.org/ebooks/1");
        inputDto.setLocNumber(null);
        inputDto.setStatusReason(null);
        BookDto returnedDto = new BookDto();
        returnedDto.setId(1L);
        returnedDto.setTitle("Test Book");
        returnedDto.setGrokipediaUrl("https://grokipedia.example.com/book/1");
        returnedDto.setFreeTextUrl("https://www.gutenberg.org/ebooks/1");
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
        inputDto.setGrokipediaUrl("https://grokipedia.example.com/book/1/updated");
        inputDto.setFreeTextUrl("https://www.gutenberg.org/ebooks/1/updated");
        inputDto.setLocNumber("Test LOC");
        inputDto.setStatusReason("Test reason");
        BookDto returnedDto = new BookDto();
        returnedDto.setId(1L);
        returnedDto.setTitle("Updated Book");
        returnedDto.setGrokipediaUrl("https://grokipedia.example.com/book/1/updated");
        returnedDto.setFreeTextUrl("https://www.gutenberg.org/ebooks/1/updated");
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
    void deleteBulkBooks() throws Exception {
        List<Long> bookIds = Arrays.asList(1L, 2L, 3L);
        doNothing().when(bookService).deleteBulkBooks(bookIds);

        mockMvc.perform(post("/api/books/delete-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookIds)))
                .andExpect(status().isOk());
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

        String response = mockMvc.perform(get("/api/books/summaries"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify that lastModified is serialized as ISO string, not array
        // Should be "2025-01-01T12:00:00" not [2025,1,1,12,0]
        assert response.contains("\"lastModified\":\"2025-01-01T12:00:00\"")
            : "Expected lastModified to be serialized as ISO string, but got: " + response;
        assert !response.contains("\"lastModified\":[2025")
            : "lastModified should not be serialized as array: " + response;
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

    @Test
    @WithMockUser
    void getBooksWithoutLocNumber() throws Exception {
        BookDto book1 = new BookDto();
        book1.setId(1L);
        book1.setTitle("Book Without LOC");
        book1.setLocNumber(null);
        book1.setDateAddedToLibrary(LocalDateTime.now());

        BookDto book2 = new BookDto();
        book2.setId(2L);
        book2.setTitle("Another Book Without LOC");
        book2.setLocNumber("");
        book2.setDateAddedToLibrary(LocalDateTime.now());

        when(bookService.getBooksWithoutLocNumber()).thenReturn(Arrays.asList(book1, book2));

        mockMvc.perform(get("/api/books/without-loc"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getBooksFromMostRecentDay() throws Exception {
        // getBooksFromMostRecentDay now returns SavedBookDto (efficient projection)
        // and includes both most recent day books AND temporary title books
        SavedBookDto book1 = SavedBookDto.builder()
                .id(1L)
                .title("Recent Book 1")
                .author("Author 1")
                .library("Library 1")
                .photoCount(2L)
                .needsProcessing(false)
                .locNumber("PS3566.O5")
                .status("ACTIVE")
                .build();

        SavedBookDto book2 = SavedBookDto.builder()
                .id(2L)
                .title("2025-01-10_14:30:00") // Temporary title - needsProcessing
                .author(null)
                .library("Library 1")
                .photoCount(1L)
                .needsProcessing(true)
                .locNumber(null)
                .status("ACTIVE")
                .build();

        when(bookService.getBooksFromMostRecentDay()).thenReturn(Arrays.asList(book1, book2));

        mockMvc.perform(get("/api/books/most-recent-day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].needsProcessing").value(false))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].needsProcessing").value(true));
    }

    @Test
    @WithMockUser
    void getBooksWith3LetterLocStart() throws Exception {
        LocalDateTime recentDate = LocalDateTime.now();
        BookDto book1 = new BookDto();
        book1.setId(1L);
        book1.setTitle("Book with ABC LOC");
        book1.setLocNumber("ABC 123.45");
        book1.setDateAddedToLibrary(recentDate);

        BookDto book2 = new BookDto();
        book2.setId(2L);
        book2.setTitle("Book with XYZ LOC");
        book2.setLocNumber("XYZ .A1");
        book2.setDateAddedToLibrary(recentDate);

        when(bookService.getBooksWith3LetterLocStart()).thenReturn(Arrays.asList(book2, book1));

        mockMvc.perform(get("/api/books/by-3letter-loc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Book with XYZ LOC"))
                .andExpect(jsonPath("$[1].title").value("Book with ABC LOC"));
    }

    @Test
    @WithMockUser
    void getBooksWith3LetterLocStartEmpty() throws Exception {
        when(bookService.getBooksWith3LetterLocStart()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/books/by-3letter-loc"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getBooksWithoutGrokipediaUrl() throws Exception {
        BookDto book1 = new BookDto();
        book1.setId(1L);
        book1.setTitle("Book Without Grokipedia URL");
        book1.setGrokipediaUrl(null);
        book1.setDateAddedToLibrary(LocalDateTime.now());

        BookDto book2 = new BookDto();
        book2.setId(2L);
        book2.setTitle("Another Book Without Grokipedia URL");
        book2.setGrokipediaUrl("");
        book2.setDateAddedToLibrary(LocalDateTime.now());

        when(bookService.getBooksWithoutGrokipediaUrl()).thenReturn(Arrays.asList(book1, book2));

        mockMvc.perform(get("/api/books/without-grokipedia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Book Without Grokipedia URL"))
                .andExpect(jsonPath("$[1].title").value("Another Book Without Grokipedia URL"));
    }

    @Test
    @WithMockUser
    void getBooksWithoutGrokipediaUrlEmpty() throws Exception {
        when(bookService.getBooksWithoutGrokipediaUrl()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/books/without-grokipedia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void cloneBook() throws Exception {
        BookDto clonedDto = new BookDto();
        clonedDto.setId(2L);
        clonedDto.setTitle("Test Book (Copy)");
        clonedDto.setGrokipediaUrl("https://grokipedia.example.com/book/2");
        clonedDto.setFreeTextUrl("https://www.gutenberg.org/ebooks/2");
        clonedDto.setLocNumber(null);
        clonedDto.setStatusReason(null);
        when(bookService.cloneBook(1L)).thenReturn(clonedDto);

        mockMvc.perform(post("/api/books/1/clone"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void suggestLocNumber() throws Exception {
        when(askGrok.suggestLocNumber("Test Book", "Test Author"))
                .thenReturn("PS3501.A1");

        Map<String, String> request = Map.of(
            "title", "Test Book",
            "author", "Test Author"
        );

        mockMvc.perform(post("/api/books/suggest-loc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void grokipediaLookupBulk() throws Exception {
        List<Long> bookIds = Arrays.asList(1L, 2L);

        GrokipediaLookupResultDto result1 = GrokipediaLookupResultDto.builder()
                .bookId(1L)
                .name("Little Women")
                .success(true)
                .grokipediaUrl("https://grokipedia.com/page/Little_Women")
                .build();

        GrokipediaLookupResultDto result2 = GrokipediaLookupResultDto.builder()
                .bookId(2L)
                .name("Unknown Book")
                .success(false)
                .errorMessage("No Grokipedia page found")
                .build();

        when(grokipediaLookupService.lookupBooks(bookIds))
                .thenReturn(Arrays.asList(result1, result2));

        mockMvc.perform(post("/api/books/grokipedia-lookup-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].success").value(true))
                .andExpect(jsonPath("$[0].grokipediaUrl").value("https://grokipedia.com/page/Little_Women"))
                .andExpect(jsonPath("$[1].success").value(false));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void grokipediaLookupBulk_requiresLibrarianAuthority() throws Exception {
        List<Long> bookIds = Arrays.asList(1L, 2L);

        mockMvc.perform(post("/api/books/grokipedia-lookup-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookIds)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void bookByPhoto() throws Exception {
        BookDto updatedBook = new BookDto();
        updatedBook.setId(1L);
        updatedBook.setTitle("AI Generated Title");
        updatedBook.setAuthorId(1L);

        when(bookService.generateTempBook(1L)).thenReturn(updatedBook);

        mockMvc.perform(put("/api/books/1/book-by-photo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("AI Generated Title"));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void bookByPhoto_requiresLibrarianAuthority() throws Exception {
        mockMvc.perform(put("/api/books/1/book-by-photo"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void bookByPhoto_returnsErrorMessage() throws Exception {
        when(bookService.generateTempBook(1L))
                .thenThrow(new RuntimeException("xAI API key not configured for user ID: 1"));

        mockMvc.perform(put("/api/books/1/book-by-photo"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("xAI API key not configured for user ID: 1"));
    }
}
