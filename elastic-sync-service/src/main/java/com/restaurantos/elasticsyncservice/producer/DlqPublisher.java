package com.restaurantos.elasticsyncservice.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topics.dlq}")
    private String dlqTopic;

    public void publish(ConsumerRecord<?, ?> record, Exception cause) {
        try {
            String key = isNull(record.key()) ? null : record.key().toString();
            String value = isNull(record.value()) ? null : record.value().toString();
            kafkaTemplate.send(dlqTopic, key, value);
            log.warn("Published to DLQ '{}': partition={}, offset={}, cause={}", dlqTopic, record.partition(), record.offset(), cause.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish record to DLQ '{}': partition={}, offset={}", dlqTopic, record.partition(), record.offset(), e);
        }
    }
}
