package ru.masterskaya.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.masterskaya.dto.PageResponse;
import ru.masterskaya.dto.RoomFilteringRequest;
import ru.masterskaya.exceptions.RoomNotFoundException;
import ru.masterskaya.model.Room;
import ru.masterskaya.repository.RoomRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

        when(roomRepository.search(10, "projector", pageable))
                .thenReturn(page);

        PageResponse<Room> result = roomService.getRooms(
                new RoomFilteringRequest(10, "projector"),
                PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals("Meeting Room", result.content().getFirst().getName());

        verify(roomRepository, times(1))
                .search(10, "projector", pageable);
    }

    @Test
    void shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(1, 5);
        Page<Room> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(roomRepository.search(null, null, pageable))
                .thenReturn(emptyPage);

        PageResponse<Room> result = roomService.getRooms(
                new RoomFilteringRequest(null, null),
                PageRequest.of(1, 5));

        assertTrue(result.isEmpty());
        verify(roomRepository).search(null, null, pageable);
    }

    @Test
    void shouldReturnRoomWhenExists() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Conference");

        when(roomRepository.findById(1L))
                .thenReturn(Optional.of(room));

        Room result = roomService.getRoomById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Conference", result.getName());

        verify(roomRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenNotFound() {
        when(roomRepository.findById(99L))
                .thenReturn(Optional.empty());

        RoomNotFoundException ex = assertThrows(
                RoomNotFoundException.class,
                () -> roomService.getRoomById(99L)
        );

        assertTrue(ex.getMessage().contains("99"));

        verify(roomRepository).findById(99L);
    }
}