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
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;

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
            throw new BusinessRuleViolationException("Connection request already exists");
        }

        if (personRepository.alreadyConnected(senderId, receiverId)) {
            throw new BusinessRuleViolationException("Users are already connected");
        }

        personRepository.addConnectionRequest(senderId, receiverId);
        log.info("Connection request added to graph DB");

        SendConnectionRequestEvent event = SendConnectionRequestEvent.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .build();

        sendRequestKafkaTemplate.send(SEND_TOPIC, event.getReceiverId(), event)
                .addCallback(kafkaCallback(SEND_TOPIC, event));

        return true;
    }

    public Boolean acceptConnectionRequest(Long senderId) {
        Long receiverId = UserContextHolder.getCurrentUserId();
        log.info("Accepting connection request from sender={} to receiver={}", senderId, receiverId);

        if (!personRepository.connectionRequestExists(senderId, receiverId)) {
            throw new BusinessRuleViolationException("No connection request to accept");
        }

        personRepository.acceptConnectionRequest(senderId, receiverId);
        log.info("Accepted connection request in graph DB");

        AcceptConnectionRequestEvent event = AcceptConnectionRequestEvent.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .build();

        acceptRequestKafkaTemplate.send(ACCEPT_TOPIC, event.getReceiverId(), event)
                .addCallback(kafkaCallback(ACCEPT_TOPIC, event));

        return true;
    }

    public Boolean rejectConnectionRequest(Long senderId) {
        Long receiverId = UserContextHolder.getCurrentUserId();

        if (!personRepository.connectionRequestExists(senderId, receiverId)) {
            throw new BusinessRuleViolationException("No connection request to reject");
        }

        personRepository.rejectConnectionRequest(senderId, receiverId);
        log.info("Rejected connection request from sender={} to receiver={}", senderId, receiverId);
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

    private <T> ListenableFutureCallback<SendResult<Long, T>> kafkaCallback(String topic, T event) {
        return new ListenableFutureCallback<>() {
            @Override
            public void onSuccess(SendResult<Long, T> result) {
                log.info("Sent event to topic {}: {}", topic, event);
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("Failed to send event to topic {}: {}", topic, event, ex);
            }
        };
    }
}
