package ru.masterskaya.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import ru.masterskaya.exceptions.RoomNotFoundException;
import ru.masterskaya.model.Room;
import ru.masterskaya.repository.RoomRepository;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    void shouldReturnPageOfRooms() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Meeting Room");

        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> page = new PageImpl<>(List.of(room), pageable, 1);

        when(roomRepository.search(10, List.of("projector"), 1, pageable))
                .thenReturn(page);

        Page<Room> result = roomService.getRooms(10, List.of("projector"), 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Meeting Room", result.getContent().get(0).getName());

        verify(roomRepository, times(1))
                .search(10, List.of("projector"), 1, pageable);
    }

    @Test
    void shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(1, 5);
        Page<Room> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(roomRepository.search(null, List.of(""), 0, pageable))
                .thenReturn(emptyPage);

        Page<Room> result = roomService.getRooms(null, null, 1, 5);

        assertTrue(result.isEmpty());
        verify(roomRepository).search(null, List.of(""), 0, pageable);
    }

    @Test
    void shouldReturnRoomWhenExists() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Conference");

        when(roomRepository.findByIdWithEquipment(1L))
                .thenReturn(Optional.of(room));

        Room result = roomService.getRoomById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Conference", result.getName());

        verify(roomRepository).findByIdWithEquipment(1L);
    }

    @Test
    void shouldThrowExceptionWhenNotFound() {
        when(roomRepository.findByIdWithEquipment(99L))
                .thenReturn(Optional.empty());

        RoomNotFoundException ex = assertThrows(
                RoomNotFoundException.class,
                () -> roomService.getRoomById(99L)
        );

        assertTrue(ex.getMessage().contains("99"));

        verify(roomRepository).findByIdWithEquipment(99L);
    }
}