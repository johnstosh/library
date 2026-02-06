/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.BranchDto;
import com.muczynski.library.dto.BranchStatisticsDto;
import com.muczynski.library.service.BranchService;
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
class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BranchService branchService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void createBranch() throws Exception {
        BranchDto inputDto = new BranchDto();
        inputDto.setBranchName("St. Martin de Porres");
        inputDto.setLibrarySystemName("library.muczynskifamily.com");
        BranchDto returnedDto = new BranchDto();
        returnedDto.setId(1L);
        returnedDto.setBranchName("St. Martin de Porres");
        returnedDto.setLibrarySystemName("library.muczynskifamily.com");
        when(branchService.createBranch(any(BranchDto.class))).thenReturn(returnedDto);

        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void getAllBranches() throws Exception {
        BranchDto dto = new BranchDto();
        dto.setId(1L);
        dto.setBranchName("St. Martin de Porres");
        when(branchService.getAllBranches()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getBranchById() throws Exception {
        BranchDto branchDto = new BranchDto();
        branchDto.setId(1L);
        branchDto.setBranchName("St. Martin de Porres");
        when(branchService.getBranchById(1L)).thenReturn(branchDto);

        mockMvc.perform(get("/api/branches/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void updateBranch() throws Exception {
        BranchDto inputDto = new BranchDto();
        inputDto.setBranchName("Updated Library");
        inputDto.setLibrarySystemName("updated.example.com");
        BranchDto returnedDto = new BranchDto();
        returnedDto.setId(1L);
        returnedDto.setBranchName("Updated Library");
        returnedDto.setLibrarySystemName("updated.example.com");
        when(branchService.updateBranch(eq(1L), any(BranchDto.class))).thenReturn(returnedDto);

        mockMvc.perform(put("/api/branches/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteBranch() throws Exception {
        doNothing().when(branchService).deleteBranch(1L);

        mockMvc.perform(delete("/api/branches/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void getBranchStatistics() throws Exception {
        BranchStatisticsDto statsDto = new BranchStatisticsDto(
                1L,
                "St. Martin de Porres",
                150L,
                12L
        );
        when(branchService.getBranchStatistics()).thenReturn(Collections.singletonList(statsDto));

        mockMvc.perform(get("/api/branches/statistics"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void getBranchStatistics_accessDeniedForNonLibrarian() throws Exception {
        // USER authority should not be able to access statistics
        mockMvc.perform(get("/api/branches/statistics"))
                .andExpect(status().isForbidden());
    }
}
