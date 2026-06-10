package ru.masterskaya;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

//@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@SpringBootTest()
@ActiveProfiles("test")
class BookingServiceAppTest {
    @Test
    void contextLoads() {
    }
}

