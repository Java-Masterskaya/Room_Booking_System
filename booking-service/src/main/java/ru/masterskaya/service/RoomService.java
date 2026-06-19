package ru.masterskaya.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.masterskaya.dto.PageResponse;
import ru.masterskaya.dto.RoomFilteringRequest;
import ru.masterskaya.dto.RoomResponse;
import ru.masterskaya.exceptions.RoomNotFoundException;
import ru.masterskaya.model.Room;
import ru.masterskaya.repository.RoomRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;

    @Cacheable(value = "rooms")
    public PageResponse<RoomResponse> getRooms(RoomFilteringRequest filteringRequest, Pageable pageable) {

        List<String> normalizedEquipment = normalizeEquipment(filteringRequest.equipment());

        log.info(
                "Выбор комнат с фильтрами: minCapacity={}, equipment={}, page={}, size={}",
                filteringRequest.minCapacity(),
                normalizedEquipment,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<Room> rooms = normalizedEquipment.isEmpty()
                ? roomRepository.searchWithoutEquipment(filteringRequest.minCapacity(), pageable)
                : roomRepository.search(
                        filteringRequest.minCapacity(),
                        normalizedEquipment,
                        normalizedEquipment.size(),
                        pageable
                );

        log.info("Найдено {} комнат на текущей странице. Всего: {}",
                rooms.getNumberOfElements(),
                rooms.getTotalElements());

        return PageResponse.from(rooms.map(RoomResponse::from));
    }

    @Cacheable(value = "rooms")
    public RoomResponse getRoomById(Long id) {

        log.info("Получение переговорной по id: {}", id);

        Room room = roomRepository.findByIdWithEquipment(id)
                .orElseThrow(() -> {
                    log.error("Переговорная с id {} не найдена", id);
                    return new RoomNotFoundException("Переговорная с id: " + id + " не найдена");
                });

        log.info("Переговорная найдена: {}, {}", room.getName(), room.getId());

        return RoomResponse.from(room);
    }

    private List<String> normalizeEquipment(List<String> equipment) {
        if (equipment == null || equipment.isEmpty()) {
            return List.of();
        }
        return equipment.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .distinct()
                .toList();
    }
}