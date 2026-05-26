package ru.masterskaya.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.masterskaya.model.Room;
import ru.masterskaya.security.JwtAuthenticationFilter;
import ru.masterskaya.security.JwtService;
import ru.masterskaya.service.RoomService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturnPage() throws Exception {

        Room room = new Room();
        room.setId(1L);
        room.setName("Room A");

        Page<Room> page = new PageImpl<>(
                List.of(room),
                PageRequest.of(0, 10),
                1
        );

        when(roomService.getRooms(null, null, 0, 10))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Room A"));

        verify(roomService).getRooms(null, null, 0, 10);
    }

    @Test
    void shouldReturnRoom() throws Exception {

        Room room = new Room();
        room.setId(1L);
        room.setName("Conference");

        when(roomService.getRoomById(1L)).thenReturn(room);

        mockMvc.perform(get("/api/v1/rooms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Conference"));

        verify(roomService).getRoomById(1L);
    }
}