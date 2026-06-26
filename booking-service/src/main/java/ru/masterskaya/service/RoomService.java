package ru.masterskaya.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Cacheable(value = "room", key = "#id")
    public Room getRoomById(Long id) {
        log.info("Получение переговорной по id: {}", id);
        return findRoomById(id);
    }

    // Временный метод заглушка, нужно будет дописывать логику
    @Caching(evict = {
            @CacheEvict(value = "rooms", allEntries = true), // Список всегда очищаем
            @CacheEvict(value = "room", key = "#id")    // Удаляем только эту комнату
    })
    public void deleteRoomById(Long id) {
        roomRepository.deleteById(id);
    }

    // Временный метод заглушка, нужно будет дописывать логику
    @CacheEvict(value = "rooms", allEntries = true) // Список всегда очищаем
    public Room createRoom(Room room) {
        return roomRepository.save(room);
    }

    // Временный метод заглушка, нужно будет дописывать логику
    @Caching(evict = {
            @CacheEvict(value = "rooms", allEntries = true), // Список всегда очищаем
            @CacheEvict(value = "room", key = "#room.id")    // Удаляем только эту комнату
    })
    @Transactional
    public Room updateRoom(Room room) {
        Room updatedRoom = findRoomById(room.getId());
        updateRoomFields(updatedRoom, room);
        return updatedRoom;
    }

    private Room findRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Переговорная с id {} не найдена", id);
                    return new RoomNotFoundException("Переговорная с id: " + id + " не найдена");
                });
    }

    // Временный метод заглушка, нужно будет дописывать логику особенно когда будут изменены на DTO
    private void updateRoomFields(Room existingRoom, Room newRoom) {
        if (!newRoom.getName().isEmpty() &&
            !newRoom.getName().isBlank() &&
            !newRoom.getName().equals(existingRoom.getName())) {
            existingRoom.setName(newRoom.getName());
        }
        if (newRoom.getCapacity() != existingRoom.getCapacity()) {
            existingRoom.setCapacity(newRoom.getCapacity());
        }
        if (newRoom.getEquipment() != null &&
            !newRoom.getEquipment().isEmpty()) {
            existingRoom.setEquipment(newRoom.getEquipment());
        }
    }
}
