package ru.masterskaya.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.masterskaya.model.Equipment;
import ru.masterskaya.model.Room;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class RoomRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url",
                postgres::getJdbcUrl);

        registry.add("spring.datasource.username",
                postgres::getUsername);

        registry.add("spring.datasource.password",
                postgres::getPassword);
    }

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void shouldFindRoomBySingleEquipment() {

        Equipment projector = new Equipment(null, "projector", null);
        Equipment board = new Equipment(null, "board", null);

        Room room = new Room(null, "Conference", 10, Set.of(projector, board));

        roomRepository.save(room);

        var result = roomRepository.search(
                null,
                List.of("projector"),
                1,
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("Conference",
                result.getContent().get(0).getName());
        assertTrue(result.getContent().get(0).getEquipmentNames().contains("projector"));
    }

    @Test
    void shouldFindRoomWhenAllEquipmentPresent() {

        Equipment projector = new Equipment(null, "projector", null);
        Equipment board = new Equipment(null, "board", null);

        Room room = new Room(null, "Conference", 10, Set.of(projector, board));
        roomRepository.save(room);

        var result = roomRepository.search(
                null,
                List.of("projector", "board"),
                2,
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("Conference", result.getContent().get(0).getName());
    }

    @Test
    void shouldNotFindRoomWhenEquipmentMissing() {

        Equipment projector = new Equipment(null, "projector", null);

        Room room = new Room(null, "Small", 5, Set.of(projector));
        roomRepository.save(room);

        var result = roomRepository.search(
                null,
                List.of("projector", "board"),
                2,
                PageRequest.of(0, 10)
        );

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void shouldFindRoomWhenExtraEquipmentPresent() {

        Equipment projector = new Equipment(null, "projector", null);
        Equipment board = new Equipment(null, "board", null);
        Equipment tv = new Equipment(null, "tv", null);

        Room room = new Room(null, "Large", 20, Set.of(projector, board, tv));
        roomRepository.save(room);

        var result = roomRepository.search(
                null,
                List.of("projector", "board"),
                2,
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("Large", result.getContent().get(0).getName());
    }

    @Test
    void shouldFindRoomCaseInsensitive() {

        Equipment projector = new Equipment(null, "Projector", null);

        Room room = new Room(null, "Alpha", 8, Set.of(projector));
        roomRepository.save(room);

        var result = roomRepository.search(
                null,
                List.of("projector"),
                1,
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("Alpha", result.getContent().get(0).getName());
    }
}
