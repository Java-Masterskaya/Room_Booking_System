package ru.masterskaya.controller;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.masterskaya.model.Booking;
import ru.masterskaya.model.Role;
import ru.masterskaya.model.User;
import ru.masterskaya.repository.BookingRepository;
import ru.masterskaya.repository.UserRepository;
import ru.masterskaya.security.JwtService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты для BookingController.
 * Использует встроенную БД H2 для тестирования.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FieldDefaults(level = AccessLevel.PRIVATE)
@DisplayName("BookingController Integration Tests")
@TestPropertySource(properties = {
        "logging.level.org.springframework.security=DEBUG",
        "logging.level.org.springframework.security.web.FilterChainProxy=DEBUG"
})
class BookingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtService jwtService;

    // Тестовые данные
    final String ownerEmail = "owner@example.com";
    final String ownerName = "Owner User";
    final String ownerPassword = "ownerPassword123";

    final String anotherUserEmail = "another@example.com";
    final String anotherUserName = "Another User";
    final String anotherPassword = "anotherPassword123";

    User ownerUser;
    User anotherUser;

    @BeforeEach
    void setUp() {
        // Очищаем БД перед каждым тестом
        bookingRepository.deleteAll();
        userRepository.deleteAll();

        // Создаем владельца бронирования
        ownerUser = User.builder()
                .email(ownerEmail)
                .name(ownerName)
                .password(passwordEncoder.encode(ownerPassword))
                .role(Role.USER)
                .build();
        ownerUser = userRepository.save(ownerUser);

        // Создаем второго пользователя
        anotherUser = User.builder()
                .email(anotherUserEmail)
                .name(anotherUserName)
                .password(passwordEncoder.encode(anotherPassword))
                .role(Role.USER)
                .build();
        anotherUser = userRepository.save(anotherUser);
    }

    @AfterEach
    void cleanUp() {
        bookingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("DELETE /api/v1/bookings/{id}")
    class DeleteBookingTests {

        @Test
        @DisplayName("Успешное удаление бронирования владельцем должно вернуть 204 NO_CONTENT")
        void deleteBooking_whenOwnerDeletesOwnBooking_thenReturnsNoContent() throws Exception {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusHours(1);
            LocalDateTime endTime = startTime.plusHours(2);

            Booking booking = new Booking(ownerUser.getId(), 1L, startTime, endTime);
            booking = bookingRepository.save(booking);

            String token = jwtService.generateToken(ownerUser);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/bookings/{id}", booking.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            // Verify
            assertThat(bookingRepository.findById(booking.getId())).isEmpty();
        }

        @Test
        @DisplayName("Удаление бронирования другим пользователем должно вернуть 403 FORBIDDEN")
        void deleteBooking_whenAnotherUserTriesToDelete_thenReturnsForbidden() throws Exception {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusHours(1);
            LocalDateTime endTime = startTime.plusHours(2);

            Booking booking = new Booking(ownerUser.getId(), 1L, startTime, endTime);
            booking = bookingRepository.save(booking);

            String tokenOfAnotherUser = jwtService.generateToken(anotherUser);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/bookings/{id}", booking.getId())
                            .header("Authorization", "Bearer " + tokenOfAnotherUser))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Forbidden"))
                    .andExpect(jsonPath("$.message").exists());

            // Verify - бронирование не должно быть удалено
            assertThat(bookingRepository.findById(booking.getId())).isPresent();
        }

        @Test
        @DisplayName("Удаление несуществующего бронирования должно вернуть 404")
        void deleteBooking_whenBookingDoesNotExist_thenReturnsInternalServerError() throws Exception {
            // Arrange
            Long nonExistentBookingId = 99999L;
            String token = jwtService.generateToken(ownerUser);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/bookings/{id}", nonExistentBookingId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Удаление бронирования с невалидным ID должно вернуть 400 BAD_REQUEST")
        void deleteBooking_whenIdIsInvalid_thenReturnsBadRequest() throws Exception {
            // Arrange
            String token = jwtService.generateToken(ownerUser);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/bookings/{id}", "invalid-id")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Удаление бронирования с отрицательным ID должно вернуть 400 BAD_REQUEST")
        void deleteBooking_whenIdIsNegative_thenReturnsBadRequest() throws Exception {
            // Arrange
            String token = jwtService.generateToken(ownerUser);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/bookings/{id}", "-1")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Удаление бронирования без токена должно вернуть 401 UNAUTHORIZED")
        void deleteBooking_whenNoAuthorizationToken_thenReturnsUnauthorized() throws Exception {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusHours(1);
            LocalDateTime endTime = startTime.plusHours(2);

            Booking booking = new Booking(ownerUser.getId(), 1L, startTime, endTime);
            booking = bookingRepository.save(booking);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/bookings/{id}", booking.getId()))
                    .andExpect(status().isUnauthorized());

            // Verify - бронирование не должно быть удалено
            assertThat(bookingRepository.findById(booking.getId())).isPresent();
        }

        @Test
        @DisplayName("Множественные удаления бронирований должны работать корректно")
        void deleteBooking_whenDeletingMultipleBookings_thenAllShouldBeDeleted() throws Exception {
            // Arrange
            LocalDateTime now = LocalDateTime.now();

            Booking booking1 = new Booking(ownerUser.getId(), 1L, now.plusHours(1), now.plusHours(2));
            Booking booking2 = new Booking(ownerUser.getId(), 2L, now.plusHours(3), now.plusHours(4));
            booking1 = bookingRepository.save(booking1);
            booking2 = bookingRepository.save(booking2);

            String token = jwtService.generateToken(ownerUser);

            // Act & Assert - удаляем первое бронирование
            mockMvc.perform(delete("/api/v1/bookings/{id}", booking1.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            assertThat(bookingRepository.findById(booking1.getId())).isEmpty();
            assertThat(bookingRepository.findById(booking2.getId())).isPresent();

            // Act & Assert - удаляем второе бронирование
            mockMvc.perform(delete("/api/v1/bookings/{id}", booking2.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            assertThat(bookingRepository.findById(booking2.getId())).isEmpty();
        }

        @Test
        @DisplayName("Удаление одного бронирования не должно влиять на другие")
        void deleteBooking_whenDeletingOneBooking_thenOthersRemainUntouched() throws Exception {
            // Arrange
            LocalDateTime now = LocalDateTime.now();

            // Два бронирования одного пользователя
            Booking ownerBooking1 = new Booking(ownerUser.getId(), 1L, now.plusHours(1), now.plusHours(2));
            Booking ownerBooking2 = new Booking(ownerUser.getId(), 3L, now.plusHours(5), now.plusHours(6));

            // Одно бронирование другого пользователя
            Booking anotherUserBooking = new Booking(anotherUser.getId(), 2L, now.plusHours(3), now.plusHours(4));

            ownerBooking1 = bookingRepository.save(ownerBooking1);
            ownerBooking2 = bookingRepository.save(ownerBooking2);
            anotherUserBooking = bookingRepository.save(anotherUserBooking);

            String ownerToken = jwtService.generateToken(ownerUser);

            // Act - удаляем первое бронирование владельца
            mockMvc.perform(delete("/api/v1/bookings/{id}", ownerBooking1.getId())
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            // Assert - первое удалено, остальные остались
            assertThat(bookingRepository.findById(ownerBooking1.getId())).isEmpty();
            assertThat(bookingRepository.findById(ownerBooking2.getId())).isPresent();
            assertThat(bookingRepository.findById(anotherUserBooking.getId())).isPresent();
        }

        @Test
        @DisplayName("Удаление бронирования с неверным токеном должно вернуть 401 UNAUTHORIZED")
        void deleteBooking_whenTokenIsInvalid_thenReturnsUnauthorized() throws Exception {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusHours(1);
            LocalDateTime endTime = startTime.plusHours(2);

            Booking booking = new Booking(ownerUser.getId(), 1L, startTime, endTime);
            booking = bookingRepository.save(booking);

            String invalidToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJub25leGlzdGVudCJ9.invalid_signature";

            // Act & Assert
            mockMvc.perform(delete("/api/v1/bookings/{id}", booking.getId())
                            .header("Authorization", "Bearer " + invalidToken))
                    .andExpect(status().isUnauthorized());

            // Verify - бронирование не должно быть удалено
            assertThat(bookingRepository.findById(booking.getId())).isPresent();
        }

        @Test
        @DisplayName("Удаление бронирования должно удалять только целевое бронирование")
        void deleteBooking_whenDeletedByOwner_thenOnlyTargetBookingIsDeleted() throws Exception {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            Booking booking1 = new Booking(ownerUser.getId(), 1L, now.plusHours(1), now.plusHours(2));
            Booking booking2 = new Booking(ownerUser.getId(), 1L, now.plusHours(3), now.plusHours(4));

            booking1 = bookingRepository.save(booking1);
            booking2 = bookingRepository.save(booking2);

            long initialCount = bookingRepository.count();
            assertThat(initialCount).isEqualTo(2);

            String token = jwtService.generateToken(ownerUser);

            // Act
            mockMvc.perform(delete("/api/v1/bookings/{id}", booking1.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            // Assert
            long finalCount = bookingRepository.count();
            assertThat(finalCount).isEqualTo(1);
            assertThat(bookingRepository.findById(booking2.getId())).isPresent();
        }

        @Test
        @DisplayName("Удаление начавшейся брони должно вернуть 403 FORBIDDEN")
        void deleteBooking_whenAlreadyStarted_thenReturnsForbidden() throws Exception {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = startTime.plusHours(2);

            Booking booking = new Booking(ownerUser.getId(), 1L, startTime, endTime);
            booking = bookingRepository.save(booking);

            String token = jwtService.generateToken(ownerUser);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/bookings/{id}", booking.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());

            // Verify
            assertThat(bookingRepository.findById(booking.getId())).isPresent();
        }

        @Test
        @DisplayName("Удаление завершенной брони должно вернуть 403 FORBIDDEN")
        void deleteBooking_whenAlreadyFinished_thenReturnsForbidden() throws Exception {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().minusHours(2);
            LocalDateTime endTime = startTime.plusHours(1);

            Booking booking = new Booking(ownerUser.getId(), 1L, startTime, endTime);
            booking = bookingRepository.save(booking);

            String token = jwtService.generateToken(ownerUser);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/bookings/{id}", booking.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());

            // Verify
            assertThat(bookingRepository.findById(booking.getId())).isPresent();
        }

    }
}

