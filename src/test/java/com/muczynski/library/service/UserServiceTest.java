// (c) Copyright 2026 by Muczynski
package com.muczynski.library.service;

import com.muczynski.library.domain.User;
import com.muczynski.library.dto.CreateUserDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.mapper.UserMapper;
import com.muczynski.library.repository.AuthorityRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailingListService mailingListService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("patron");
        user.setEmail("old@example.com");
        user.setSsoProvider("local");
    }

    @Test
    void updateUser_rejectsEmailChangeForSsoUser() {
        user.setSsoProvider("google");
        CreateUserDto dto = new CreateUserDto();
        dto.setEmail("new@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateUser(1L, dto))
                .isInstanceOf(LibraryException.class)
                .hasMessageContaining("Email cannot be changed for an SSO user");
        assertThat(user.getEmail()).isEqualTo("old@example.com");
        verify(userRepository, never()).save(any());
        verify(mailingListService, never()).moveEmail(any(), any());
    }

    @Test
    void updateUser_rejectsEmailChangeWhenOnlySubjectIdIsSet() {
        user.setSsoSubjectId("subject-1");
        CreateUserDto dto = new CreateUserDto();
        dto.setEmail("new@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateUser(1L, dto))
                .isInstanceOf(LibraryException.class)
                .hasMessageContaining("Email cannot be changed for an SSO user");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_ssoUserCanSendTheSameEmailOrOmitIt() {
        user.setSsoProvider("google");
        user.setSsoSubjectId("subject-1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toDto(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            UserDto dto = new UserDto();
            dto.setEmail(saved.getEmail());
            dto.setPhone(saved.getPhone());
            return dto;
        });
        when(loanRepository.countByUserIdAndReturnDateIsNull(1L)).thenReturn(0L);

        CreateUserDto same = new CreateUserDto();
        same.setEmail("old@example.com");
        assertThat(userService.updateUser(1L, same).getEmail()).isEqualTo("old@example.com");

        CreateUserDto omitted = new CreateUserDto();
        omitted.setPhone("555-0100");
        UserDto result = userService.updateUser(1L, omitted);
        assertThat(result.getEmail()).isEqualTo("old@example.com");
        assertThat(result.getPhone()).isEqualTo("555-0100");
        verify(mailingListService, never()).moveEmail(any(), any());
    }

    @Test
    void updateUser_localUserCanChangeEmail() {
        CreateUserDto dto = new CreateUserDto();
        dto.setEmail("new@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toDto(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            UserDto mapped = new UserDto();
            mapped.setEmail(saved.getEmail());
            return mapped;
        });
        when(loanRepository.countByUserIdAndReturnDateIsNull(1L)).thenReturn(0L);

        UserDto result = userService.updateUser(1L, dto);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(mailingListService).moveEmail("old@example.com", "new@example.com");
    }
}
