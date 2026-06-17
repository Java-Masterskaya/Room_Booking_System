package ru.masterskaya.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import ru.masterskaya.model.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(value = {
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")
    })
    @Query("""
    SELECT e FROM OutboxEvent AS e
    WHERE e.status = 'NEW'
     ORDER BY createdAt ASC
    """)
    List<OutboxEvent> findNewEvents(Pageable pageable);
}
