package com.restaurantos.elasticservice.service.impl;

import com.restaurantos.elasticservice.client.EnrichmentClient;
import com.restaurantos.elasticservice.config.IndexResolver;
import com.restaurantos.elasticservice.config.SourceProperties;
import com.restaurantos.elasticservice.config.SourceProperties.Source;
import com.restaurantos.elasticservice.dto.ChangeEvent;
import com.restaurantos.elasticservice.dto.IndexDocument;
import com.restaurantos.elasticservice.enums.ChangeEventOperation;
import com.restaurantos.elasticservice.service.IndexService;
import com.restaurantos.elasticservice.service.SyncService;
import com.restaurantos.elasticservice.util.ChangeEventParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.restaurantos.elasticservice.util.ChangeEventParser.documentId;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {

    private final IndexService indexService;
    private final IndexResolver indexResolver;
    private final EnrichmentClient enrichmentClient;
    private final ChangeEventParser changeEventParser;
    private final SourceProperties sourceProperties;

    @Override
    public void process(List<ConsumerRecord<String, String>> records) {
        // A batch may span topics; each topic maps to one configured source.
        Map<String, List<ConsumerRecord<String, String>>> byTopic = new LinkedHashMap<>();
        for (ConsumerRecord<String, String> record : records) {
            byTopic.computeIfAbsent(record.topic(), t -> new ArrayList<>()).add(record);
        }
        byTopic.forEach(this::processTopic);
    }

    private void processTopic(String topic, List<ConsumerRecord<String, String>> records) {
        Source source = sourceProperties.forTopic(topic);
        if (isNull(source)) {
            log.error("No sync source configured for topic '{}'; skipping {} record(s)", topic, records.size());
            return;
        }

        List<ChangeEvent> events = changeEventParser.parseAndFilter(records);
        if (isEmpty(events)) {
            log.debug("Topic {}: all {} records were tombstones or unparseable", topic, records.size());
            return;
        }

        Set<String> upsertIds = new LinkedHashSet<>();
        Set<String> deleteIds = new LinkedHashSet<>();
        for (ChangeEvent event : events) {
            if (event.getOperation() == ChangeEventOperation.DELETE) {
                String id = documentId(event.getBefore());
                if (nonNull(id)) {
                    deleteIds.add(id);
                    upsertIds.remove(id);
                }
            } else {
                String id = documentId(event.getAfter());
                if (nonNull(id)) {
                    upsertIds.add(id);
                    deleteIds.remove(id);
                }
            }
        }

        List<IndexDocument> documents = enrichmentClient.fetch(source, upsertIds);

        // Ids we asked to upsert but the source no longer returns were deleted between the CDC event
        // and our lookup — drop them from the index.
        Set<String> returned = new LinkedHashSet<>();
        documents.forEach(document -> returned.add(document.getId()));
        upsertIds.forEach(id -> {
            if (!returned.contains(id)) deleteIds.add(id);
        });

        String prefix = source.getIndexPrefix();
        indexService.bulkSave(documents, document -> indexResolver.resolve(prefix, document.getRoutingKey()));
        indexService.bulkDelete(deleteIds, indexResolver.pattern(prefix));

        log.debug("Sync complete [{}]: {} upserts, {} deletes (from {} records)",
                topic, documents.size(), deleteIds.size(), records.size());
    }
}
