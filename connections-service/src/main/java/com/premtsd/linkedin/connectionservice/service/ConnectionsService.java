package com.premtsd.linkedin.connectionservice.service;

import com.premtsd.linkedin.connectionservice.auth.UserContextHolder;
import com.premtsd.linkedin.connectionservice.entity.Person;
import com.premtsd.linkedin.connectionservice.event.AcceptConnectionRequestEvent;
import com.premtsd.linkedin.connectionservice.event.SendConnectionRequestEvent;
import com.premtsd.linkedin.connectionservice.exception.BusinessRuleViolationException;
import com.premtsd.linkedin.connectionservice.repository.PersonRepository;
import com.premtsd.linkedin.userservice.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
@Slf4j
public class ConnectionsService {

    private final PersonRepository personRepository;
    private final KafkaTemplate<Long, SendConnectionRequestEvent> sendRequestKafkaTemplate;
    private final KafkaTemplate<Long, AcceptConnectionRequestEvent> acceptRequestKafkaTemplate;

    private static final String SEND_TOPIC = "send-connection-request-topic";
    private static final String ACCEPT_TOPIC = "accept-connection-request-topic";

    @KafkaListener(topics = "user-created-topic", groupId = "connection-service")
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("Received user creation event: {}", event);
        Person person = new Person();
        person.setUserId(event.getUserId());
        person.setName(event.getName());
        personRepository.save(person);
    }

    public List<Person> getFirstDegreeConnections() {
        Long userId = UserContextHolder.getCurrentUserId();
        log.info("Fetching first-degree connections for userId: {}", userId);
        return personRepository.getFirstDegreeConnections(userId);
    }

    public Boolean sendConnectionRequest(Long receiverId) {
        Long senderId = UserContextHolder.getCurrentUserId();
        log.info("Attempting to send connection request: sender={}, receiver={}", senderId, receiverId);

        validateSenderAndReceiver(senderId, receiverId);

        if (personRepository.connectionRequestExists(senderId, receiverId)) {
            log.warn("Connection request already exists: sender={}, receiver={}", senderId, receiverId);
            throw new BusinessRuleViolationException("Connection request already exists");
        }

        if (personRepository.alreadyConnected(senderId, receiverId)) {
            log.warn("Users are already connected: sender={}, receiver={}", senderId, receiverId);
            throw new BusinessRuleViolationException("Users are already connected");
        }

        log.debug("Adding connection request to graph database");
        personRepository.addConnectionRequest(senderId, receiverId);
        log.info("Connection request added to graph DB successfully");

        log.debug("Publishing connection request event");
        SendConnectionRequestEvent event = SendConnectionRequestEvent.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .build();

        CompletableFuture<SendResult<Long, SendConnectionRequestEvent>> future =
                sendRequestKafkaTemplate.send(SEND_TOPIC, event.getReceiverId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent connection request event to topic {}: {}", SEND_TOPIC, event);
            } else {
                log.error("Failed to send connection request event to topic {}: {}", SEND_TOPIC, event, ex);
            }
        });

        log.info("Connection request process completed: sender={}, receiver={}", senderId, receiverId);
        return true;
    }

    public Boolean acceptConnectionRequest(Long senderId) {
        Long receiverId = UserContextHolder.getCurrentUserId();
        log.info("Accepting connection request from sender={} to receiver={}", senderId, receiverId);

        if (!personRepository.connectionRequestExists(senderId, receiverId)) {
            log.warn("No connection request exists to accept: sender={}, receiver={}", senderId, receiverId);
            throw new BusinessRuleViolationException("No connection request to accept");
        }

        log.debug("Accepting connection request in graph database");
        personRepository.acceptConnectionRequest(senderId, receiverId);
        log.info("Accepted connection request in graph DB successfully");

        log.debug("Publishing accept connection request event");
        AcceptConnectionRequestEvent event = AcceptConnectionRequestEvent.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .build();

        CompletableFuture<SendResult<Long, AcceptConnectionRequestEvent>> future =
                acceptRequestKafkaTemplate.send(ACCEPT_TOPIC, event.getReceiverId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent accept connection request event to topic {}: {}", ACCEPT_TOPIC, event);
            } else {
                log.error("Failed to send accept connection request event to topic {}: {}", ACCEPT_TOPIC, event, ex);
            }
        });

        log.info("Accept connection request process completed: sender={}, receiver={}", senderId, receiverId);
        return true;
    }

    public Boolean rejectConnectionRequest(Long senderId) {
        Long receiverId = UserContextHolder.getCurrentUserId();
        log.info("Rejecting connection request from sender={} to receiver={}", senderId, receiverId);

        if (!personRepository.connectionRequestExists(senderId, receiverId)) {
            log.warn("No connection request exists to reject: sender={}, receiver={}", senderId, receiverId);
            throw new BusinessRuleViolationException("No connection request to reject");
        }

        log.debug("Rejecting connection request in graph database");
        personRepository.rejectConnectionRequest(senderId, receiverId);
        log.info("Connection request rejected successfully: sender={}, receiver={}", senderId, receiverId);
        return true;
    }

    private void validateSenderAndReceiver(Long senderId, Long receiverId) {
        if (senderId == null || receiverId == null) {
            throw new IllegalArgumentException("Sender or receiver ID cannot be null");
        }

        if (senderId.equals(receiverId)) {
            throw new BusinessRuleViolationException("Sender and receiver cannot be the same user");
        }
    }


}
