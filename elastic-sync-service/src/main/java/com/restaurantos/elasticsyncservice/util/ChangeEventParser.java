package com.restaurantos.elasticsyncservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantos.elasticsyncservice.dto.ChangeEvent;
import com.restaurantos.elasticsyncservice.dto.ChangeRecord;
import com.restaurantos.elasticsyncservice.producer.DlqPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

/**
 * Reusable CDC-record plumbing shared by any sync consumer: deserializes Debezium change events,
 * skips tombstones, and routes unparseable records to the DLQ. Keeping this out of the concrete
 * {@code SyncService} lets future sync flows (other tables/indices) reuse the same parsing contract.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChangeEventParser {

    private final ObjectMapper objectMapper;
    private final DlqPublisher dlqPublisher;

    public List<ChangeEvent> parseAndFilter(List<ConsumerRecord<String, String>> records) {
        List<ChangeEvent> events = new ArrayList<>(records.size());
        for (ConsumerRecord<String, String> record : records) {
            if (isNull(record.value())) {
                log.debug("Skipping tombstone: partition={}, offset={}", record.partition(), record.offset());
                continue;
            }
            try {
                ChangeEvent event = objectMapper.readValue(record.value(), ChangeEvent.class);
                if (isNull(event.getOperation())) throw new IllegalStateException("Change event has no 'op' field");
                events.add(event);
            } catch (Exception e) {
                log.error("Parse failure — routing to DLQ: partition={}, offset={}, error={}", record.partition(), record.offset(), e.getMessage());
                dlqPublisher.publish(record, e);
            }
        }
        return events;
    }

    public static String documentId(ChangeRecord record) {
        if (isNull(record)) return null;
        return record.getId();
    }
}
