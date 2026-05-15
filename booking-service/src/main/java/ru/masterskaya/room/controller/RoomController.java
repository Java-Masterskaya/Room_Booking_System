package ru.masterskaya.room.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.masterskaya.room.model.Room;

import java.util.*;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final Map<Integer, Room> repository = new HashMap<>();

    public RoomController() {
        Room testRoom = new Room();
        testRoom.setId(1);
        testRoom.setName("Альфа");
        testRoom.setCapacity(10);
        testRoom.setEquipment(Arrays.asList("Проектор", "Маркерная доска"));
        repository.put(testRoom.getId(), testRoom);
    }

    @GetMapping
    public List<Room> getRooms() {
        return new ArrayList<>(repository.values());
    }
}
