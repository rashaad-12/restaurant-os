package com.restaurantos.elasticsyncservice.service.impl;

import com.restaurantos.elasticsyncservice.dto.IndexDocument;
import com.restaurantos.elasticsyncservice.repository.ESCustomRepository;
import com.restaurantos.elasticsyncservice.service.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final ESCustomRepository repository;

    @Override
    public void bulkSave(List<IndexDocument> documents, Function<IndexDocument, String> indexOf) {
        if (documents.isEmpty()) return;
        try {
            repository.bulkSave(documents, indexOf);
            log.debug("Bulk upserted {} document(s) across tenant indices", documents.size());
        } catch (Exception e) {
            throw new RuntimeException("ES bulk upsert failed for " + documents.size() + " document(s)", e);
        }
    }

    @Override
    public void bulkDelete(Collection<String> ids, String indexPattern) {
        if (ids.isEmpty()) return;
        try {
            repository.bulkDelete(ids, indexPattern);
            log.debug("Bulk deleted {} document(s) across {}", ids.size(), indexPattern);
        } catch (Exception e) {
            throw new RuntimeException("ES bulk delete failed for " + ids.size() + " id(s)", e);
        }
    }
}
