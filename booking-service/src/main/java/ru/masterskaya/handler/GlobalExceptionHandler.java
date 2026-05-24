package ru.masterskaya.handler;

import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.springmvc.InvalidRequestException;
import com.atlassian.oai.validator.springmvc.InvalidResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import ru.masterskaya.dto.ApiErrorResponse;
import ru.masterskaya.exceptions.CustomInvalidRequestException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(CustomInvalidRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomInvalidRequestException(CustomInvalidRequestException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "Schema request validation failed",
                exception.getMaskedErrors()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }
}
