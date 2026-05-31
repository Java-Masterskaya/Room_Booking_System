package ru.masterskaya.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.masterskaya.annotation.LogAllMethods;
import ru.masterskaya.dto.auth.AuthRequest;
import ru.masterskaya.dto.auth.AuthResponse;
import ru.masterskaya.dto.auth.RegisterRequest;
import ru.masterskaya.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@LogAllMethods
public class AuthController {
    final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/authenticate")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse authenticate(@Valid @RequestBody AuthRequest request) {
        return authService.authenticate(request);
    }

}
