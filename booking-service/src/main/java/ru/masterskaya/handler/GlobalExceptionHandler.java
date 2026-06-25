package ru.masterskaya.handler;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import ru.masterskaya.dto.ApiErrorResponse;
import ru.masterskaya.exceptions.*;

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

    @ExceptionHandler(BookingAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleBookingAccessDeniedException(BookingAccessDeniedException exception) {
        log.warn("Доступ к бронированию запрещен: {}", exception.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN,
                "У вас нет прав для выполнения данного действия с этим бронированием.",
                exception.getMessage());
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientAuthenticationException(InsufficientAuthenticationException exception) {
        log.warn("Учетные данные аутентификации не полные или отсутствуют: {}", exception.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Не полные или отсутствующие данные аутентификации",
                exception.getMessage());
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ApiErrorResponse> handleSignatureException(SignatureException exception) {
        log.warn("Неверная подпись у токена: {}", exception.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Неверная подпись у токена",
                exception.getMessage());
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJwtException(MalformedJwtException exception) {
        log.warn("Неверный формат JWT токена: {}", exception.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Неверный формат JWT токена",
                exception.getMessage());
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiErrorResponse> handleExpiredJwtException(ExpiredJwtException exception) {
        log.warn("JWT токен истек: {}", exception.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED,
                "JWT токен истек",
                exception.getMessage());
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleBookingNotFoundException(BookingNotFoundException exception) {
        log.warn("Бронирование не найдено: {}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND,
                "Запрошенное бронирование не существует в системе.",
                exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        log.warn("Превышен лимит размера тела запроса или файла: {}", exception.getMessage());
        return buildResponse(HttpStatus.CONTENT_TOO_LARGE,
                "The request body size exceeds the allowed limit",
                exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception) {
        log.error("Ошибка типа параметра: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Ошибка типа параметра", exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception) {
        log.error("Constraint violation error: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Constraint violation error", exception.getMessage());
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