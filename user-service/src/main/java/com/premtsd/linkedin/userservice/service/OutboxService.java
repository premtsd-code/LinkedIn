package com.premtsd.linkedin.userservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.premtsd.linkedin.userservice.entity.OutboxEvent;
import com.premtsd.linkedin.userservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Stages an event for publication. MUST be called inside the same
     * transaction as the business change it announces — that is the whole
     * point of the outbox pattern.
     */
    public void enqueue(String topic, Object event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // Not fatal (the insert still commits on its own), but it forfeits
            // the atomicity guarantee — surface it loudly during development.
            log.warn("OutboxService.enqueue called outside a transaction for topic {}", topic);
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.of(topic, event.getClass().getName(), payload);
            outboxEventRepository.save(outboxEvent);
            log.debug("Staged outbox event {} for topic {}", outboxEvent.getId(), topic);
        } catch (JsonProcessingException e) {
            // Programming error (unserializable event) — fail the business tx.
            throw new IllegalArgumentException("Cannot serialize event for topic " + topic, e);
        }
    }
}
