package com.premtsd.linkedin.notificationservice.service;

import com.premtsd.linkedin.notificationservice.entity.ProcessedEvent;
import com.premtsd.linkedin.notificationservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventDeduplicator {

    private final ProcessedEventRepository processedEventRepository;

    /**
     * True if this event was already fully processed (i.e. this is an
     * at-least-once redelivery). Events without an eventId header (producers
     * not yet using the outbox) are never treated as duplicates.
     */
    public boolean alreadyProcessed(String eventId) {
        if (eventId == null) {
            return false;
        }
        boolean seen = processedEventRepository.existsById(eventId);
        if (seen) {
            log.info("Skipping duplicate delivery of event {}", eventId);
        }
        return seen;
    }

    /**
     * Records the event as processed. Call inside the SAME transaction as the
     * event's side effects, so marker and effects commit or roll back together.
     */
    public void markProcessed(String eventId) {
        if (eventId != null) {
            processedEventRepository.save(new ProcessedEvent(eventId));
        }
    }
}
