package ru.masterskaya.dto;

import java.time.OffsetDateTime;

public record BookingRequest(
        Integer roomId,
        Integer userId,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
    public boolean isPeriodInvalid() {
        return !startTime.isBefore(endTime);
    }
}
