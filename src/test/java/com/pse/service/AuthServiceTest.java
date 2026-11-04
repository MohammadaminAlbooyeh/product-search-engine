package com.pse.service;

import com.pse.exception.EmailAlreadyRegisteredException;
import com.pse.model.dto.AuthResponse;
import com.pse.model.dto.LoginRequest;
import com.pse.model.dto.RegisterRequest;
import com.pse.model.entity.User;
import com.pse.model.enums.UserRole;
import com.pse.repository.UserRepository;
import com.pse.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);

    @Test
    void registerNormalizesEmailHashesPasswordAndIssuesToken() {
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.issue(any(User.class))).thenReturn(AuthResponse.builder().token("jwt").build());

        AuthResponse response = authService.register(RegisterRequest.builder()
                .email("  NEW@example.com ").password("password123").displayName("New").build());

        assertThat(response.getToken()).isEqualTo("jwt");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hash");
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(RegisterRequest.builder()
                .email("dup@example.com").password("password123").build()))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginIssuesTokenWhenPasswordMatches() {
        User user = User.builder().id(2L).email("user@example.com")
                .passwordHash("hash").role(UserRole.USER).enabled(true).build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(jwtService.issue(user)).thenReturn(AuthResponse.builder().token("jwt").build());

        AuthResponse response = authService.login(LoginRequest.builder()
                .email("USER@example.com").password("password123").build());

        assertThat(response.getToken()).isEqualTo("jwt");
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownUser() {
        User user = User.builder().id(2L).email("user@example.com")
                .passwordHash("hash").role(UserRole.USER).enabled(true).build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(LoginRequest.builder()
                .email("user@example.com").password("wrong").build()))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> authService.login(LoginRequest.builder()
                .email("missing@example.com").password("x").build()))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void disabledUserCannotLogin() {
        User user = User.builder().id(2L).email("user@example.com")
                .passwordHash("hash").role(UserRole.USER).enabled(false).build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(LoginRequest.builder()
                .email("user@example.com").password("password123").build()))
                .isInstanceOf(BadCredentialsException.class);
    }
}
