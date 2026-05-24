package ru.masterskaya;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@Slf4j
public class BookingServiceApp {
    public static void main(String[] args) {
        log.info("Starting Booking Service");
        SpringApplication.run(BookingServiceApp.class, args);
        log.info("Booking Service started");
    }
}
