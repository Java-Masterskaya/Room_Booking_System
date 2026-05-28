package ru.masterskaya.service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.masterskaya.dto.auth.AuthRequest;
import ru.masterskaya.dto.auth.AuthResponse;
import ru.masterskaya.dto.auth.RegisterRequest;
import ru.masterskaya.exceptions.EmailExistException;
import ru.masterskaya.model.Role;
import ru.masterskaya.model.User;
import ru.masterskaya.repository.UserRepository;
import ru.masterskaya.security.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class AuthServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    Authentication authentication;

    @InjectMocks
    AuthService authService;

    RegisterRequest registerRequest;
    AuthRequest authRequest;
    User user;
    final String testEmail = "test@example.com";
    final String testName = "test";
    final String testPassword = "test123";
    final String encodedPassword = "encodedTest123";
    final String jwtToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0";

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setName(testName);
        registerRequest.setPassword(testPassword);

        authRequest = new AuthRequest();
        authRequest.setEmail(testEmail);
        authRequest.setPassword(testPassword);

        user = User.builder()
                .id(1L)
                .email(testEmail)
                .name(testName)
                .password(encodedPassword)
                .role(Role.USER)
                .build();
    }

    @Test
    void registerSuccess() {
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn(jwtToken);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(jwtToken);
        assertThat(response.getEmail()).isEqualTo(testEmail);
        assertThat(response.getRole()).isEqualTo(Role.USER.name());
    }

    @Test
    void registerEmailExistsThrowsException() {
        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailExistException.class)
                .hasMessage("Email уже существует");
    }

    @Test
    void authenticateSuccess() {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(testEmail, testPassword);

        when(authenticationManager.authenticate(authToken)).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(jwtToken);

        AuthResponse response = authService.authenticate(authRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(jwtToken);
        assertThat(response.getEmail()).isEqualTo(testEmail);
    }

    @Test
    void authenticateBadCredentialsThrowsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.authenticate(authRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");
    }
}