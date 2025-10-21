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
import static org.mockito.ArgumentMatchers.eq;
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
        fullUserDto.setRoles(Collections.singleton("USER"));
        fullUserDto.setXaiApiKey("test-key");
        when(userService.getUserByUsername("testuser")).thenReturn(fullUserDto);

        mockMvc.perform(get("/api/users/me")
                        .with(user("testuser").authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles").value("USER"))
                .andExpect(jsonPath("$.xaiApiKey").value("test-key"));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void getAllUsers() throws Exception {
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setUsername("Test User");
        dto.setRoles(Collections.singleton("USER"));
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
        userDto.setRoles(Collections.singleton("USER"));
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
        inputDto.setRole("USER");
        UserDto returnedDto = new UserDto();
        returnedDto.setId(1L);
        returnedDto.setUsername("Test User");
        returnedDto.setRoles(Collections.singleton("USER"));
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
        inputDto.setRole("LIBRARIAN");
        UserDto returnedDto = new UserDto();
        returnedDto.setId(1L);
        returnedDto.setUsername("Updated User");
        returnedDto.setRoles(Collections.singleton("LIBRARIAN"));
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
}
