package ru.masterskaya.controller;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.masterskaya.dto.auth.AuthRequest;
import ru.masterskaya.dto.auth.AuthResponse;
import ru.masterskaya.dto.auth.RegisterRequest;
import ru.masterskaya.model.Role;
import ru.masterskaya.model.User;
import ru.masterskaya.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FieldDefaults(level = AccessLevel.PRIVATE)
class AuthControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    RegisterRequest registerRequest;
    AuthRequest authRequest;

    final String testEmail = "test@example.com";
    final String testName = "test";
    final String testPassword = "test123";

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setName(testName);
        registerRequest.setPassword(testPassword);

        authRequest = new AuthRequest();
        authRequest.setEmail(testEmail);
        authRequest.setPassword(testPassword);
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerSuccess() throws Exception {
        String responseJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.role").value(Role.USER.name()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse response = objectMapper.readValue(responseJson, AuthResponse.class);
        assertThat(response.getToken()).isNotBlank();

        User savedUser = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo(testEmail);
        assertThat(savedUser.getName()).isEqualTo(testName);
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(passwordEncoder.matches(testPassword, savedUser.getPassword())).isTrue();
    }

    @Test
    void registerEmailAlreadyExistsReturnsConflictRequest() throws Exception {
        User existingUser = User.builder()
                .email(testEmail)
                .name(testName)
                .password(passwordEncoder.encode(testPassword))
                .role(Role.USER)
                .build();
        userRepository.save(existingUser);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email уже существует"));
    }

    @Test
    void registerInvalidEmailReturnsBadRequest() throws Exception {
        registerRequest.setEmail("invalid email");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_PasswordTooShort_ReturnsBadRequest() throws Exception {
        registerRequest.setPassword("test");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticateSuccess() throws Exception {
        User user = User.builder()
                .email(testEmail)
                .name(testName)
                .password(passwordEncoder.encode(testPassword))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.role").value(Role.USER.name()));
    }

    @Test
    void authenticateWrongPasswordReturnsUnauthorized() throws Exception {
        User user = User.builder()
                .email(testEmail)
                .name(testName)
                .password(passwordEncoder.encode(testPassword))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        authRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticateEmailNotFoundReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());
    }
}