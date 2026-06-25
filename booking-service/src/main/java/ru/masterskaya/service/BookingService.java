package ru.masterskaya.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.masterskaya.exceptions.BookingAccessDeniedException;
import ru.masterskaya.exceptions.BookingNotFoundException;
import ru.masterskaya.model.Booking;
import ru.masterskaya.model.User;
import ru.masterskaya.repository.BookingRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CurrentUserService currentUserService;


    public void deleteBooking(Long id) {
        User currentUser = currentUserService.getCurrentUser();
        Booking booking = getBooking(id);
        LocalDateTime now = LocalDateTime.now();
        if (!booking.getUserId().equals(currentUser.getId())) {
            throw new BookingAccessDeniedException("Нельзя отменить бронирование другого пользователя");
        }
        if (now.isAfter(booking.getEndTime()) || now.isAfter(booking.getStartTime())) {
            throw new BookingAccessDeniedException("Нельзя отменить бронирование, которое уже началось или завершилось");
        }
        bookingRepository.delete(booking);
    }


    private Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Бронирование не найдено, id: " + id));
    }
}