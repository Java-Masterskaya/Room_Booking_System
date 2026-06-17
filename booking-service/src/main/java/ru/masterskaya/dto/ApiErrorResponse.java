package ru.masterskaya.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ApiErrorResponse {
    @Builder.Default
    private final String timestamp = Instant.now().toString();
    private final int status;
    private final String error;
    private final String message;
    private final List<String> details;
}
