package com.premtsd.linkedin.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premtsd.linkedin.userservice.entity.OutboxEvent;
import com.premtsd.linkedin.userservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publishes staged outbox rows to Kafka. Runs outside the business
 * transaction, retries forever (a row stays unpublished until the broker
 * acks it), and is crash-safe: worst case a row is published twice, which is
 * why every record carries an "eventId" header consumers can deduplicate on.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    public static final String EVENT_ID_HEADER = "eventId";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<Long, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.relay-interval-ms:2000}")
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }
        log.info("Outbox relay: publishing {} pending event(s)", pending.size());
        for (OutboxEvent event : pending) {
            try {
                // Rebuild the original event object so the existing JsonSerializer
                // (and its __TypeId__ header) behaves exactly as before the outbox.
                Object payload = objectMapper.readValue(event.getPayload(), Class.forName(event.getEventType()));
                ProducerRecord<Long, Object> record = new ProducerRecord<>(event.getTopic(), payload);
                record.headers().add(EVENT_ID_HEADER, event.getId().toString().getBytes(StandardCharsets.UTF_8));

                kafkaTemplate.send(record).get(10, TimeUnit.SECONDS); // wait for broker ack

                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                event.setLastError(null);
                outboxEventRepository.save(event);
                log.info("Outbox relay: published event {} to {}", event.getId(), event.getTopic());
            } catch (Exception e) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(abbreviate(e.getMessage()));
                outboxEventRepository.save(event);
                log.warn("Outbox relay: failed to publish event {} (attempt {}): {}",
                        event.getId(), event.getAttempts(), e.getMessage());
                // Stop this round; order-preserving and avoids hammering a dead broker.
                return;
            }
        }
    }

    private String abbreviate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
