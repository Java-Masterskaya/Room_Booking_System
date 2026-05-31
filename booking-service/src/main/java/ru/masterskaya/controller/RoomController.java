package ru.masterskaya.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import ru.masterskaya.annotation.LogAllMethods;
import ru.masterskaya.dto.PageResponse;
import ru.masterskaya.dto.RoomFilteringRequest;
import ru.masterskaya.model.Room;
import ru.masterskaya.service.RoomService;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@LogAllMethods
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public PageResponse<Room> getRooms(
            @ModelAttribute RoomFilteringRequest filteringRequest,
            @PageableDefault(size = 10, page = 0)
            Pageable pageable
    ) {
        return roomService.getRooms(filteringRequest, pageable);
    }

    @GetMapping("/{id}")
    public Room getRoomById(@PathVariable Long id) {
        return roomService.getRoomById(id);
    }
}
