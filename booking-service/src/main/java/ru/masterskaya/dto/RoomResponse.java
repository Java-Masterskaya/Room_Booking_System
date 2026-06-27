package ru.masterskaya.dto;

import lombok.Builder;
import ru.masterskaya.model.Equipment;
import ru.masterskaya.model.Room;

import java.util.Comparator;
import java.util.List;

@Builder
public record RoomResponse(
        Long id,
        String name,
        int capacity,
        List<String> equipment
) {

    public static RoomResponse from(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .equipment(room.getEquipment().stream()
                        .map(Equipment::getName)
                        .sorted(Comparator.naturalOrder())
                        .toList())
                .build();
    }
}
