package ru.masterskaya.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import ru.masterskaya.dto.ApiErrorResponse;
import ru.masterskaya.exceptions.BookingConflictException;
import ru.masterskaya.exceptions.CustomInvalidRequestException;
import ru.masterskaya.exceptions.EmailExistException;
import ru.masterskaya.exceptions.RoomNotFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();

        log.warn("Ошибка валидации параметров запроса: {}", details);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(CustomInvalidRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomInvalidRequestException(CustomInvalidRequestException exception) {
        List<String> details = exception.getMaskedErrors();
        log.warn("Ошибка валидации по OpenAPI спецификации: {}", exception.getMaskedErrors());
        return buildResponse(HttpStatus.BAD_REQUEST, "Schema request validation failed", details);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentialsException(BadCredentialsException exception) {
        log.warn("Неудачная попытка аутентификации: {}", exception.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Неверный email или пароль",
                "Предоставлены неверные учетные данные.");
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRoomNotFoundException(RoomNotFoundException exception) {
        log.warn("Комната не найдена: {}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND,
                "Комната не найдена",
                "Запрошенная комната не существует в системе.");
    }

    @ExceptionHandler(EmailExistException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailExistException(EmailExistException exception) {
        log.warn("Пользователь с таким Email уже существует: {}", exception.getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "Email уже существует",
                "Пользователь с таким адресом электронной почты уже зарегистрирован.");
    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleBookingConflictException(BookingConflictException exception) {
        log.warn("Такая бронь уже существует: {}", exception.getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "Конфликт (комната уже занята на это время)",
                "Выбранный временной интервал для данной комнаты уже забронирован.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        log.warn("Превышен лимит размера тела запроса или файла: {}", exception.getMessage());
        return buildResponse(HttpStatus.CONTENT_TOO_LARGE,
                "The request body size exceeds the allowed limit",
                exception.getMessage());
    }

    @ExceptionHandler({Exception.class, RuntimeException.class, Throwable.class})
    public ResponseEntity<ApiErrorResponse> handleAllUncaughtExceptions(Exception exception) {
        log.error("Критическая непредвиденная ошибка на сервере: ", exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", List.of());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status,
                                                           String message,
                                                           String details) {
        List<String> detailsList = (details != null && !details.isBlank()) ? List.of(details) : List.of();
        return buildResponse(status, message, detailsList);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status,
                                                           String message,
                                                           List<String> details) {
        ApiErrorResponse apiErrorResponse = ApiErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .details(details)
                .build();
        return ResponseEntity.status(status).body(apiErrorResponse);
    }
}