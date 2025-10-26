// (c) Copyright 2025 by Muczynski
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.LibraryDto;
import com.muczynski.library.service.LibraryService;
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
class LibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibraryService libraryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void createLibrary() throws Exception {
        LibraryDto inputDto = new LibraryDto();
        inputDto.setName("Test Library");
        inputDto.setHostname("test.example.com");
        LibraryDto returnedDto = new LibraryDto();
        returnedDto.setId(1L);
        returnedDto.setName("Test Library");
        returnedDto.setHostname("test.example.com");
        when(libraryService.createLibrary(any(LibraryDto.class))).thenReturn(returnedDto);

        mockMvc.perform(post("/api/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void getAllLibraries() throws Exception {
        LibraryDto dto = new LibraryDto();
        dto.setId(1L);
        dto.setName("Test Library");
        when(libraryService.getAllLibraries()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/libraries"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getLibraryById() throws Exception {
        LibraryDto libraryDto = new LibraryDto();
        libraryDto.setId(1L);
        libraryDto.setName("Test Library");
        when(libraryService.getLibraryById(1L)).thenReturn(libraryDto);

        mockMvc.perform(get("/api/libraries/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void updateLibrary() throws Exception {
        LibraryDto inputDto = new LibraryDto();
        inputDto.setName("Updated Library");
        inputDto.setHostname("updated.example.com");
        LibraryDto returnedDto = new LibraryDto();
        returnedDto.setId(1L);
        returnedDto.setName("Updated Library");
        returnedDto.setHostname("updated.example.com");
        when(libraryService.updateLibrary(eq(1L), any(LibraryDto.class))).thenReturn(returnedDto);

        mockMvc.perform(put("/api/libraries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteLibrary() throws Exception {
        doNothing().when(libraryService).deleteLibrary(1L);

        mockMvc.perform(delete("/api/libraries/1"))
                .andExpect(status().isNoContent());
    }
}
