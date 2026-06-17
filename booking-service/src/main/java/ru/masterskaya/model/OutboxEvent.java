package ru.masterskaya.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    public static final String AGGREGATE_TYPE_BOOKING = "BOOKING";
    public static final String EVENT_BOOKING_CREATED = "BOOKING_CREATED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", columnDefinition  = "jsonb",  nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String payLoad;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payLoad, OutboxStatus status) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payLoad = payLoad;
        this.status = status;
    }
}
