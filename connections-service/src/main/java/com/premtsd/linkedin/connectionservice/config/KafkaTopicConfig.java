package com.premtsd.linkedin.connectionservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka topic configuration for connection-related events.
 * Topics are created automatically at application startup (if auto-creation is enabled).
 */
@Configuration
public class KafkaTopicConfig {

    // Constants to avoid magic strings and reuse across services
    public static final String SEND_CONNECTION_REQUEST_TOPIC = "send-connection-request-topic";
    public static final String ACCEPT_CONNECTION_REQUEST_TOPIC = "accept-connection-request-topic";

    private static final int NUM_PARTITIONS = 3;
    private static final short REPLICATION_FACTOR = 1;

    @Bean
    public NewTopic sendConnectionRequestTopic() {
        return new NewTopic(SEND_CONNECTION_REQUEST_TOPIC, NUM_PARTITIONS, REPLICATION_FACTOR);
    }

    @Bean
    public NewTopic acceptConnectionRequestTopic() {
        return new NewTopic(ACCEPT_CONNECTION_REQUEST_TOPIC, NUM_PARTITIONS, REPLICATION_FACTOR);
    }
}
