package ru.masterskaya.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.masterskaya.dto.BookingRequest;
import ru.masterskaya.service.BookingService;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<Void> createBooking(
            @Valid @RequestBody BookingRequest bookingRequest) {

        bookingService.createBooking(bookingRequest);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
