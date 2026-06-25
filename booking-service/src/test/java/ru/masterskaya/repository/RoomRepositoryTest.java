package ru.masterskaya.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.masterskaya.model.Room;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void shouldFindRoomByEquipment() {

        Room room = new Room(
                null,
                "Conference",
                10,
                List.of("projector", "board")
        );

        roomRepository.save(room);

        var result = roomRepository.search(
                null,
                "projector",
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("Conference",
                result.getContent().get(0).getName());
    }
}