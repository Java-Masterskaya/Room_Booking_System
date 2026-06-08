package ru.masterskaya.openApiDemoBooking;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Временный контроллер для демонстрации и тестирования работы
 * OpenAPI-валидации, лимитов body, CORS и маскирования секретов в логах.
 * Будет удален после появления боевых эндпоинтов.
 */

@RestController
@RequestMapping("/api/v1/bookingsdemo")
public class OpenApiDemoBookingController {

    private final List<OpenApiDemoBookingRequest> bookings = new ArrayList<>();

    @PostMapping
    public ResponseEntity<String> createBooking(@RequestBody OpenApiDemoBookingRequest request) {
        System.out.println(request);
        bookings.add(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Бронирование создано");
    }

    @GetMapping
    public List<OpenApiDemoBookingRequest> getBookings() {
        return bookings;
    }
}
