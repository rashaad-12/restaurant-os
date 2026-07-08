package com.restaurantos.elasticservice.service;

import com.restaurantos.elasticservice.dto.IndexDocument;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public interface IndexService {

    /** Indexes opaque documents; {@code indexOf} resolves each document's target index. */
    void bulkSave(List<IndexDocument> documents, Function<IndexDocument, String> indexOf);

    /** Deletes documents by id across the given index pattern. */
    void bulkDelete(Collection<String> ids, String indexPattern);
}
