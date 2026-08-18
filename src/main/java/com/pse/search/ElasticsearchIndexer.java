package com.pse.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pse.model.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexer {

    private final ElasticsearchOperations elasticsearchOperations;

    public boolean index(ProductDocument document) {
        try {
            elasticsearchOperations.save(document);
            return true;
        } catch (Exception e) {
            log.warn("Elasticsearch unavailable, skipped indexing {}: {}", document.getId(), e.getMessage());
            return false;
        }
    }

    public void delete(String productId) {
        try {
            elasticsearchOperations.delete(productId, IndexCoordinates.of("products"));
        } catch (Exception e) {
            log.warn("Elasticsearch unavailable, skipped delete {}: {}", productId, e.getMessage());
        }
    }
}
