package ru.masterskaya.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.masterskaya.model.Booking;

import java.time.LocalDateTime;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            SELECT COUNT(b)>0
            FROM Booking AS b
            WHERE b.roomId=:roomId
             AND b.startTime<:endTime
             AND b.endTime>:startTime
            """)
    boolean existsOverlapping(Long roomId, LocalDateTime startTime, LocalDateTime endTime);
}
