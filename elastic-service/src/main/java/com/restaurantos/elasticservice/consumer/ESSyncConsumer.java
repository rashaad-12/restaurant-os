package com.restaurantos.elasticservice.consumer;

import com.restaurantos.elasticservice.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ESSyncConsumer {

    private final SyncService syncService;

    @KafkaListener(
            topics = "#{@sourceProperties.topics()}",
            containerFactory = "syncListenerContainerFactory")
    public void onMessage(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        log.debug("Order change batch received: size={}", records.size());
        try {
            syncService.process(records);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Order change batch failed (will retry): size={}, error={}", records.size(), e.getMessage(), e);
            throw e;
        }
    }
}
