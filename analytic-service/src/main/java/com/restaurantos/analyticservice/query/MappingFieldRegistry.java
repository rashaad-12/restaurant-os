package com.restaurantos.analyticservice.query;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.FieldCapsResponse;
import com.restaurantos.analyticservice.enums.FieldType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MappingFieldRegistry {

    private final ElasticsearchClient client;
    private final Map<String, FieldTypes> cache = new ConcurrentHashMap<>();

    public FieldTypes forIndex(String index) {
        return cache.computeIfAbsent(index, this::load);
    }

    private FieldTypes load(String index) {
        Map<String, FieldType> types = new HashMap<>();
        Set<String> nested = new HashSet<>();
        try {
            FieldCapsResponse response = client.fieldCaps(f -> f
                    .index(index)
                    .fields("*")
                    .ignoreUnavailable(true)
                    .allowNoIndices(true));

            for (var entry : response.fields().entrySet()) {
                String field = entry.getKey();
                String esType = entry.getValue().keySet().iterator().next();
                if ("nested".equals(esType)) {
                    nested.add(field);
                } else if (!"object".equals(esType)) {
                    types.put(field, mapType(esType));
                }
            }
        } catch (Exception e) {
            log.warn("Could not load field caps for index '{}' ({}); defaulting field types to KEYWORD", index, e.getMessage());
        }
        return new FieldTypes(types, nested);
    }

    private FieldType mapType(String esType) {
        return switch (esType) {
            case "text", "match_only_text", "search_as_you_type" -> FieldType.TEXT;
            case "date", "date_nanos" -> FieldType.DATE;
            case "boolean" -> FieldType.BOOLEAN;
            case "long", "integer", "short", "byte", "unsigned_long" -> FieldType.LONG;
            case "double", "float", "half_float", "scaled_float" -> FieldType.DOUBLE;
            default -> FieldType.KEYWORD;
        };
    }
}
