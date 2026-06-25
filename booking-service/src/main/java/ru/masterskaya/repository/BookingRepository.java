package ru.masterskaya.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.masterskaya.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

}
