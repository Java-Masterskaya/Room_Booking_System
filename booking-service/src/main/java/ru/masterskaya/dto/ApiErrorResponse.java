package ru.masterskaya.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ApiErrorResponse {
    private final String timestamp = LocalDateTime.now().toString();
    private final int status;
    private final String error;
    private final String message;
    private final List<String> details;

    public ApiErrorResponse(int status, String error, String message, List<String> details) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.details = details;
    }
}
