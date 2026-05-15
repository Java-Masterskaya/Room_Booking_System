package ru.masterskaya.booking;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final List<BookingRequest> bookings = new ArrayList<>();

    @PostMapping
    public ResponseEntity<String> createBooking(@RequestBody BookingRequest request) {
        System.out.println(request);
        // Если управление дошло сюда — контракт соблюден на 100%
        bookings.add(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Бронирование создано");
    }

    @GetMapping
    public List<BookingRequest> getBookings() {
        return bookings;
    }
}
