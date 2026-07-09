package com.restaurantos.elasticsyncservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-source sync configuration. Each source binds a CDC topic to the enrichment endpoint that
 * produces its documents and to the index prefix used for tenant indices. Adding a new source (any
 * future service) is config only — no code changes in this platform service.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sync")
public class SourceProperties {

    private Map<String, Source> sources = new LinkedHashMap<>();

    public Source forTopic(String topic) {
        return sources.values().stream()
                .filter(source -> topic.equals(source.getTopic()))
                .findFirst()
                .orElse(null);
    }

    public List<String> topics() {
        return sources.values().stream().map(Source::getTopic).toList();
    }

    @Getter
    @Setter
    public static class Source {
        private String topic;
        private String indexPrefix;
        private Enrichment enrichment = new Enrichment();
    }

    @Getter
    @Setter
    public static class Enrichment {
        private String baseUrl;
        private String path;
    }
}
