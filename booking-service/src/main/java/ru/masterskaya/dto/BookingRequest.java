package ru.masterskaya.dto;

import jakarta.validation.constraints.AssertTrue;

import java.time.OffsetDateTime;

public record BookingRequest(
        Integer roomId,
        Integer userId,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
    @AssertTrue(message = "Ошибка валидации: время окончания должно быть строго позже времени начала")
    public boolean isPeriodValid() {
        return startTime.isBefore(endTime);
    }
}
