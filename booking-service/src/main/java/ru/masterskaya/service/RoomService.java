package ru.masterskaya.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.masterskaya.exceptions.RoomNotFoundException;
import ru.masterskaya.model.Room;
import ru.masterskaya.repository.RoomRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;

    public Page<Room> getRooms(
            Integer minCapacity,
            String equipment,
            int page,
            int size
    ) {

        log.info(
                "Выбор комнат с фильтрами: minCapacity={}, equipment={}, page={}, size={}",
                minCapacity,
                equipment,
                page,
                size
        );

        Pageable pageable = PageRequest.of(page, size);

        Page<Room> rooms = roomRepository.search(
                minCapacity,
                equipment,
                pageable
        );

        log.info("Найдено {} комнат на текущей странице. Всего: {}",
                rooms.getNumberOfElements(),
                rooms.getTotalElements());

        return rooms;
    }

    public Room getRoomById(Long id) {

        log.info("Получение переговорной по id: {}", id);

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> {

                    log.error("Переговорная с id {} не найдена", id);

                    return new RoomNotFoundException("Переговорная с id: " + id + " не найдена");
                });

        log.info("Переговорная найдена: {}, {}", room.getName(), room.getId());

        return room;
    }
}
