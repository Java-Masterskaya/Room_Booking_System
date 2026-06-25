package ru.masterskaya.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.masterskaya.exceptions.BookingAccessDeniedException;
import ru.masterskaya.model.Booking;
import ru.masterskaya.model.User;
import ru.masterskaya.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void deleteBooking_whenOwner_thenDeletesBooking() {
        Long bookingId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUserId(10L);
        booking.setStartTime(now.plusHours(1));
        booking.setEndTime(now.plusHours(2));

        User currentUser = new User();
        currentUser.setId(10L);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        bookingService.deleteBooking(bookingId);

        verify(bookingRepository, times(1)).delete(booking);
    }

    @Test
    void deleteBooking_whenNotOwner_thenThrowsAccessDenied() {
        Long bookingId = 2L;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUserId(11L);

        User currentUser = new User();
        currentUser.setId(99L);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        assertThrows(BookingAccessDeniedException.class, () -> bookingService.deleteBooking(bookingId));

        verify(bookingRepository, never()).delete(any());
    }

    @Test
    void deleteBooking_whenAlreadyStarted_thenThrowsAccessDenied() {
        Long bookingId = 3L;
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setUserId(20L);
        // booking already started one hour ago
        booking.setStartTime(now.minusHours(1));
        booking.setEndTime(now.plusHours(1));

        User currentUser = new User();
        currentUser.setId(20L);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        assertThrows(BookingAccessDeniedException.class, () -> bookingService.deleteBooking(bookingId));

        verify(bookingRepository, never()).delete(any());
    }
}

