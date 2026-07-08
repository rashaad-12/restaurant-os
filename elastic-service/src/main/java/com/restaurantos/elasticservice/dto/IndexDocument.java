package com.restaurantos.elasticservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

/**
 * Generic, opaque document returned by a source's enrichment endpoint. The platform routes it by
 * {@code routingKey} (tenant/org) and indexes {@code body} verbatim under {@code id} — it never
 * inspects the body. The source service owns the body shape.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndexDocument {

    private String id;
    private String routingKey;
    private JsonNode body;
}
