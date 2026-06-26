package ru.masterskaya.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.masterskaya.dto.PageResponse;
import ru.masterskaya.dto.RoomFilteringRequest;
import ru.masterskaya.model.Room;
import ru.masterskaya.security.JwtAuthenticationFilter;
import ru.masterskaya.security.JwtService;
import ru.masterskaya.service.RoomService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

        PageResponse<Room> page = PageResponse.<Room>builder()
                .page(1)
                .totalPages(10)
                .size(10)
                .totalElements(100)
                .content(List.of(room))
                .build();

        when(roomService.getRooms(
                new RoomFilteringRequest(null, null),
                PageRequest.of(0, 10)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Room A"));

        verify(roomService).getRooms(new RoomFilteringRequest(null, null),
                PageRequest.of(0, 10));
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