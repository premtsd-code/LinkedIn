package com.premtsd.linkedin.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox row: an event staged in the SAME database transaction
 * as the business change that caused it. The OutboxRelay publishes it to
 * Kafka afterwards. Commit => event will eventually be published (no lost
 * events); rollback => row disappears with the business change (no phantom
 * events).
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String topic;

    /** Fully-qualified class name of the event payload, for re-serialization. */
    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean published;

    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    private String lastError;

    public static OutboxEvent of(String topic, String eventType, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setTopic(topic);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setCreatedAt(Instant.now());
        event.setPublished(false);
        event.setAttempts(0);
        return event;
    }
}
