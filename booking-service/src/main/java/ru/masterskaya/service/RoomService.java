package ru.masterskaya.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.masterskaya.dto.PageResponse;
import ru.masterskaya.dto.RoomFilteringRequest;
import ru.masterskaya.exceptions.RoomNotFoundException;
import ru.masterskaya.model.Room;
import ru.masterskaya.repository.RoomRepository;


@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;

    @Cacheable(value = "rooms")
    public PageResponse<Room> getRooms(RoomFilteringRequest filteringRequest, Pageable pageable) {

        log.info(
                "Выбор комнат с фильтрами: minCapacity={}, equipment={}, page={}, size={}",
                filteringRequest.minCapacity(),
                filteringRequest.equipment(),
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<Room> rooms = roomRepository.search(
                filteringRequest.minCapacity(),
                filteringRequest.equipment(),
                pageable
        );

        log.info("Найдено {} комнат на текущей странице. Всего: {}",
                rooms.getNumberOfElements(),
                rooms.getTotalElements());

        return PageResponse.from(rooms);
    }

    @Cacheable(value = "rooms")
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
