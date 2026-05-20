package ru.masterskaya.exceptions;

import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.springmvc.InvalidRequestException;
import com.atlassian.oai.validator.springmvc.InvalidResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import ru.masterskaya.error.ApiErrorResponse;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleOpenApiValidationError(InvalidRequestException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        List<String> errors = exception.getValidationReport().getMessages().stream()
                .map(ValidationReport.Message::getMessage)
                .collect(Collectors.toList());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "Schema request validation failed",
                errors
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(InvalidResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleOpenApiValidationError(InvalidResponseException exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR; // 500, так как ошибся сервер

        List<String> errors = exception.getValidationReport().getMessages().stream()
                .map(ValidationReport.Message::getMessage)
                .collect(Collectors.toList());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "Schema response validation failed",
                errors
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        HttpStatus status = HttpStatus.PAYLOAD_TOO_LARGE;

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "The request body size exceeds the allowed limit",
                List.of(exception.getMessage())
        );
        return ResponseEntity.status(status).body(errorResponse);
    }
}
