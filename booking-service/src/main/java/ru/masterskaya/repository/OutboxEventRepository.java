package ru.masterskaya.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.masterskaya.model.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
    SELECT e FROM OutboxEvent AS e
    WHERE e.status = 'NEW'
     ORDER BY createdAt ASC
    """)
    List<OutboxEvent> findNewEvents(Pageable pageable);
}
