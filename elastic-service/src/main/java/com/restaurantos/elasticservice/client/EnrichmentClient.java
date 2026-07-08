package com.restaurantos.elasticservice.client;

import com.restaurantos.elasticservice.config.SourceProperties.Source;
import com.restaurantos.elasticservice.dto.IndexDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/**
 * Generic enrichment client: given a source's endpoint and a set of ids, fetches the opaque
 * {@link IndexDocument}s that source owns. It has no knowledge of any specific domain — the endpoint
 * comes from configuration and the payload is passed through verbatim.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichmentClient {

    private static final ParameterizedTypeReference<List<IndexDocument>> DOCUMENT_LIST = new ParameterizedTypeReference<>() {};

    private final RestClient enrichmentRestClient;
    private final SystemTokenProvider systemTokenProvider;

    public List<IndexDocument> fetch(Source source, Collection<String> ids) {
        if (isEmpty(ids)) return List.of();

        String url = source.getEnrichment().getBaseUrl() + source.getEnrichment().getPath();
        List<IndexDocument> documents = enrichmentRestClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + systemTokenProvider.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.copyOf(ids))
                .retrieve()
                .body(DOCUMENT_LIST);

        log.debug("Enrichment fetch [{}]: requested {} id(s), received {} document(s)",
                url, ids.size(), documents == null ? 0 : documents.size());
        return documents == null ? List.of() : documents;
    }
}
