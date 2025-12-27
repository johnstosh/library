/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.CreateUserDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCurrentUser() throws Exception {
        // Mock the full user by username
        UserDto fullUserDto = new UserDto();
        fullUserDto.setId(1L);
        fullUserDto.setUsername("testuser");
        fullUserDto.setAuthorities(Collections.singleton("USER"));
        fullUserDto.setXaiApiKey("test-key");
        when(userService.getUserByUsername("testuser")).thenReturn(fullUserDto);

        mockMvc.perform(get("/api/users/me")
                        .with(user("testuser").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.authorities").value("USER"))
                .andExpect(jsonPath("$.xaiApiKey").value("test-key"));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void getAllUsers() throws Exception {
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setUsername("Test User");
        dto.setAuthorities(Collections.singleton("USER"));
        when(userService.getAllUsers()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void getUserById() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        userDto.setUsername("Test User");
        userDto.setAuthorities(Collections.singleton("USER"));
        when(userService.getUserById(1L)).thenReturn(userDto);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void createUser() throws Exception {
        CreateUserDto inputDto = new CreateUserDto();
        inputDto.setUsername("Test User");
        inputDto.setPassword("password");
        inputDto.setAuthority("USER");
        UserDto returnedDto = new UserDto();
        returnedDto.setId(1L);
        returnedDto.setUsername("Test User");
        returnedDto.setAuthorities(Collections.singleton("USER"));
        when(userService.createUser(any(CreateUserDto.class))).thenReturn(returnedDto);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void updateUser() throws Exception {
        CreateUserDto inputDto = new CreateUserDto();
        inputDto.setUsername("Updated User");
        inputDto.setPassword("newpassword");
        inputDto.setAuthority("LIBRARIAN");
        UserDto returnedDto = new UserDto();
        returnedDto.setId(1L);
        returnedDto.setUsername("Updated User");
        returnedDto.setAuthorities(Collections.singleton("LIBRARIAN"));
        when(userService.updateUser(eq(1L), any(CreateUserDto.class))).thenReturn(returnedDto);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void registerUser_success() throws Exception {
        CreateUserDto inputDto = new CreateUserDto();
        inputDto.setUsername("newuser");
        inputDto.setPassword("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"); // SHA-256 hash
        inputDto.setAuthority("USER");
        UserDto returnedDto = new UserDto();
        returnedDto.setId(1L);
        returnedDto.setUsername("newuser");
        returnedDto.setAuthorities(Collections.singleton("USER"));
        when(userService.createUser(any(CreateUserDto.class))).thenReturn(returnedDto);

        mockMvc.perform(post("/api/users/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void registerUser_rejectsNonUserAuthority() throws Exception {
        CreateUserDto inputDto = new CreateUserDto();
        inputDto.setUsername("newuser");
        inputDto.setPassword("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        inputDto.setAuthority("LIBRARIAN"); // Should be rejected

        mockMvc.perform(post("/api/users/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void updateApiKey_success() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setXaiApiKey("xai-valid-api-key-that-is-at-least-32-characters-long");
        UserDto updatedDto = new UserDto();
        updatedDto.setId(1L);
        updatedDto.setUsername("testuser");
        updatedDto.setXaiApiKey("xai-valid-api-key-that-is-at-least-32-characters-long");
        when(userService.getUserById(1L)).thenReturn(updatedDto);

        mockMvc.perform(put("/api/users/1/apikey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xaiApiKey").value("xai-valid-api-key-that-is-at-least-32-characters-long"));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void updateApiKey_rejectsTooShort() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setXaiApiKey("short");
        doThrow(new IllegalArgumentException("XAI API key must be at least 32 characters"))
                .when(userService).updateApiKey(eq(1L), eq("short"));

        mockMvc.perform(put("/api/users/1/apikey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void getUserById_returnsNotFound_whenUserDoesNotExist() throws Exception {
        when(userService.getUserById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteUser_returnsConflict_whenUserHasActiveLoans() throws Exception {
        doThrow(new RuntimeException("Cannot delete user because they have 2 active loan(s). Please return all books before deleting the user."))
                .when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteBulkUsers_success() throws Exception {
        List<Long> ids = List.of(1L, 2L, 3L);
        doNothing().when(userService).deleteBulkUsers(anyList());

        mockMvc.perform(post("/api/users/delete-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteBulkUsers_returnsConflict_whenUserHasActiveLoans() throws Exception {
        List<Long> ids = List.of(1L, 2L);
        doThrow(new RuntimeException("Cannot delete user because they have 1 active loan(s). Please return all books before deleting the user."))
                .when(userService).deleteBulkUsers(anyList());

        mockMvc.perform(post("/api/users/delete-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }
}
