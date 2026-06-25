package ru.masterskaya.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.masterskaya.model.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query(value = """
            SELECT *
            FROM rooms
            WHERE (:capacity IS NULL OR capacity >= :capacity)
            AND (:equipment IS NULL OR :equipment = ANY(equipment))
            ORDER BY id
            """,
            countQuery = """
                    SELECT count(*)
                    FROM rooms
                    WHERE (:capacity IS NULL OR capacity >= :capacity)
                    AND (:equipment IS NULL OR :equipment = ANY(equipment))
                    """,
            nativeQuery = true)
    Page<Room> search(
            @Param("capacity") Integer capacity,
            @Param("equipment") String equipment,
            Pageable pageable
    );
}
