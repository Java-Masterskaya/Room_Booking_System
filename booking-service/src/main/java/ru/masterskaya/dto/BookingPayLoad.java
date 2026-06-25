package ru.masterskaya.dto;

public record BookingPayLoad(
        Long bookingId,
        Long userId,
        Long roomId,
        String startTime,
        String endTime
) {
}
