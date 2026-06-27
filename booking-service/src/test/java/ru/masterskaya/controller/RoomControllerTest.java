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
import ru.masterskaya.dto.RoomResponse;
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

        RoomResponse room = RoomResponse.builder()
                .id(1L)
                .name("Room A")
                .capacity(10)
                .equipment(List.of())
                .build();

        RoomFilteringRequest request = new RoomFilteringRequest(null, null);

        PageResponse<RoomResponse> page = PageResponse.<RoomResponse>builder()
                .page(0)
                .totalPages(10)
                .size(10)
                .totalElements(100)
                .content(List.of(room))
                .build();

        when(roomService.getRooms(request, PageRequest.of(0, 10)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Room A"));

        verify(roomService).getRooms(request, PageRequest.of(0, 10));
    }

    @Test
    void shouldFilterByEquipment() throws Exception {

        RoomResponse room = RoomResponse.builder()
                .id(1L)
                .name("Room A")
                .capacity(10)
                .equipment(List.of("projector"))
                .build();

        RoomFilteringRequest request = new RoomFilteringRequest(null, List.of("projector"));
        PageResponse<RoomResponse> page = new PageResponse<>(List.of(room), 0, 10, 1, 1);

        when(roomService.getRooms(request, PageRequest.of(0, 10)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/rooms")
                        .param("equipment", "projector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].equipment[0]").value("projector"));

        verify(roomService).getRooms(request, PageRequest.of(0, 10));
    }

    @Test
    void shouldReturnRoom() throws Exception {

        RoomResponse room = RoomResponse.builder()
                .id(1L)
                .name("Conference")
                .capacity(10)
                .equipment(List.of())
                .build();

        when(roomService.getRoomById(1L)).thenReturn(room);

        mockMvc.perform(get("/api/v1/rooms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Conference"));

        verify(roomService).getRoomById(1L);
    }
}