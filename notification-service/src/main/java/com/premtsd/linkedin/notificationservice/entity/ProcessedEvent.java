package com.premtsd.linkedin.notificationservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Idempotency ledger: one row per Kafka event this service has fully
 * processed. Inserted in the SAME transaction as the event's effects, so
 * "processed" and "its side effects exist" can never disagree. A redelivered
 * event whose id is already here is skipped.
 */
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEvent {

    @Id
    private String eventId;

    private Instant processedAt;

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }
}
