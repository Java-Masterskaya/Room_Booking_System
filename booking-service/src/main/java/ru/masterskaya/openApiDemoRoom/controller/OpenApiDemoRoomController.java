package ru.masterskaya.openApiDemoRoom.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.masterskaya.openApiDemoRoom.model.OpenApiDemoRoom;

import java.util.*;

/**
 * Временный контроллер для демонстрации и тестирования работы
 * OpenAPI-валидации, лимитов body, CORS и маскирования секретов в логах.
 * Будет удален после появления боевых эндпоинтов.
 */

@RestController
@RequestMapping("/api/v1/roomsdemo")
public class OpenApiDemoRoomController {

    private final Map<Integer, OpenApiDemoRoom> repository = new HashMap<>();

    public OpenApiDemoRoomController() {
        OpenApiDemoRoom testOpenApiDemoRoom = new OpenApiDemoRoom();
        testOpenApiDemoRoom.setId(1);
        testOpenApiDemoRoom.setName("Альфа");
        testOpenApiDemoRoom.setCapacity(10);
        testOpenApiDemoRoom.setEquipment(Arrays.asList("Проектор", "Маркерная доска"));
        repository.put(testOpenApiDemoRoom.getId(), testOpenApiDemoRoom);
    }

    @GetMapping
    public List<OpenApiDemoRoom> getRooms() {
        return new ArrayList<>(repository.values());
    }
}
