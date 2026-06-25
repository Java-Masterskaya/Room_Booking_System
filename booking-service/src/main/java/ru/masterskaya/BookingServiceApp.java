package ru.masterskaya;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@Slf4j
@EnableScheduling
public class BookingServiceApp {
    public static void main(String[] args) {
        log.info("Starting Booking Service");
        SpringApplication.run(BookingServiceApp.class, args);
    }
}
