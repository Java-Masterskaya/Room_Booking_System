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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;

    public Page<Room> getRooms(
            Integer minCapacity,
            List<String> equipment,
            int page,
            int size
    ) {

        List<String> normalizedEquipment = normalizeEquipment(equipment);
        int equipmentSize = normalizedEquipment.size();
        List<String> equipmentParam = equipmentSize == 0 ? List.of("") : normalizedEquipment;

        log.info(
                "Выбор комнат с фильтрами: minCapacity={}, equipment={}, page={}, size={}",
                minCapacity,
                normalizedEquipment,
                page,
                size
        );

        Pageable pageable = PageRequest.of(page, size);

        Page<Room> rooms = roomRepository.search(
                minCapacity,
                equipmentParam,
                equipmentSize,
                pageable
        );

        log.info("Найдено {} комнат на текущей странице. Всего: {}",
                rooms.getNumberOfElements(),
                rooms.getTotalElements());

        return rooms;
    }

    public Room getRoomById(Long id) {

        log.info("Получение переговорной по id: {}", id);

        Room room = roomRepository.findByIdWithEquipment(id)
                .orElseThrow(() -> {

                    log.error("Переговорная с id {} не найдена", id);

                    return new RoomNotFoundException("Переговорная с id: " + id + " не найдена");
                });

        log.info("Переговорная найдена: {}, {}", room.getName(), room.getId());

        return room;
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
