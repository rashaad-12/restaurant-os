package com.restaurantos.elasticservice.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.List;

public interface SyncService {
    void process(List<ConsumerRecord<String, String>> records);
}
