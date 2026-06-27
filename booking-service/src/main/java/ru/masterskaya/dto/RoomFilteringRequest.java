package ru.masterskaya.dto;

import java.util.List;

public record RoomFilteringRequest(
        Integer minCapacity,
        List<String> equipment
) {
}