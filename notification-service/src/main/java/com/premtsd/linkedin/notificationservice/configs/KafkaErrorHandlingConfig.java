package com.premtsd.linkedin.notificationservice.configs;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Poison-message handling for all @KafkaListener consumers in this service:
 * a failing record is retried 3 times with exponential backoff, then
 * published to "<original-topic>.DLT" (dead-letter topic) and skipped, so one
 * bad message can never block the partition.
 *
 * Spring Boot auto-wires the single CommonErrorHandler bean into the default
 * listener container factory — no factory override needed.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaProperties kafkaProperties) {
        // Failed records that deserialized fine are re-published as JSON;
        // records that failed *deserialization* arrive as raw byte[] and need
        // a byte[] template — give the recoverer both.
        Map<String, Object> base = new HashMap<>();
        base.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        base.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);

        Map<String, Object> jsonProps = new HashMap<>(base);
        jsonProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        KafkaTemplate<Object, Object> jsonTemplate =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(jsonProps));

        Map<String, Object> bytesProps = new HashMap<>(base);
        bytesProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        KafkaTemplate<Object, Object> bytesTemplate =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(bytesProps));

        Map<Class<?>, KafkaOperations<?, ?>> templates = new HashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, jsonTemplate);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(templates);

        // 1s, 2s, 4s between attempts; 4 total deliveries, then -> DLT.
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(10_000L);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
