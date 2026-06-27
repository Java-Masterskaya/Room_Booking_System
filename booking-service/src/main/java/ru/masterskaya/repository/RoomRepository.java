package ru.masterskaya.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.masterskaya.model.Room;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @EntityGraph(attributePaths = "equipment")
    @Query(value = """
        SELECT r FROM Room r
        WHERE (:capacity IS NULL OR r.capacity >= :capacity)
          AND (
            SELECT COUNT(DISTINCT LOWER(e.name))
            FROM r.equipment e
            WHERE LOWER(e.name) IN :equipment
          ) = :equipmentSize
        ORDER BY r.id
        """,
            countQuery = """
        SELECT COUNT(r) FROM Room r
        WHERE (:capacity IS NULL OR r.capacity >= :capacity)
          AND (
            SELECT COUNT(DISTINCT LOWER(e.name))
            FROM r.equipment e
            WHERE LOWER(e.name) IN :equipment
          ) = :equipmentSize
        """)
    Page<Room> search(
            @Param("capacity") Integer capacity,
            @Param("equipment") List<String> equipment,
            @Param("equipmentSize") int equipmentSize,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "equipment")
    @Query(value = """
        SELECT r FROM Room r
        WHERE (:capacity IS NULL OR r.capacity >= :capacity)
        ORDER BY r.id
        """,
            countQuery = """
        SELECT COUNT(r) FROM Room r
        WHERE (:capacity IS NULL OR r.capacity >= :capacity)
        """)
    Page<Room> searchWithoutEquipment(@Param("capacity") Integer capacity, Pageable pageable);

    @EntityGraph(attributePaths = "equipment")
    @Query(value = "SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdWithEquipment(@Param("id") Long id);
}
