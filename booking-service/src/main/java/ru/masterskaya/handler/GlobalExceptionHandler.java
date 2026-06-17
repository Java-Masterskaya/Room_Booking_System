package ru.masterskaya.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import ru.masterskaya.dto.ApiErrorResponse;
import ru.masterskaya.exceptions.BookingConflictException;
import ru.masterskaya.exceptions.CustomInvalidRequestException;

import java.util.List;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> hendleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        List<String> details = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .toList();

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "Validation failed",
                details
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    // Удалить
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
//        HttpStatus status = HttpStatus.BAD_REQUEST;
//
//        ApiErrorResponse errorResponse = new ApiErrorResponse(
//                status.value(),
//                status.getReasonPhrase(),
//                "Ошибка валидации (неверный формат данных)",
//                List.of(exception.getMessage())
//        );
//        return ResponseEntity.status(status).body(errorResponse);
//    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleBookingConflictException(BookingConflictException exception) {
        HttpStatus status = HttpStatus.CONFLICT;

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "Конфликт (комната уже занята на это время)",
                List.of(exception.getMessage())
        );

        return ResponseEntity.status(status).body(errorResponse);
    }
}

