package ru.masterskaya.dto;

public record RoomFilteringRequest(
        Integer minCapacity,
        String equipment
) {
}