package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.service.AuthorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorService authorService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void createAuthor() throws Exception {
        AuthorDto inputDto = new AuthorDto();
        inputDto.setName("Test Author");
        AuthorDto returnedDto = new AuthorDto();
        returnedDto.setId(1L);
        returnedDto.setName("Test Author");
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
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Test Author");
        when(authorService.getAuthorById(1L)).thenReturn(authorDto);

        mockMvc.perform(get("/api/authors/1"))
                .andExpect(status().isOk());
    }

}
