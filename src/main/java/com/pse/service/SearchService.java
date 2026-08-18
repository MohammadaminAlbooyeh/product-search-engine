package com.pse.service;

import com.pse.cache.SearchResultCacheService;
import com.pse.model.document.ProductDocument;
import com.pse.model.dto.SearchRequest;
import com.pse.model.dto.SearchResponse;
import com.pse.model.entity.PriceEntry;
import com.pse.model.entity.Product;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.model.enums.ProductCategory;
import com.pse.repository.PriceEntryRepository;
import com.pse.repository.ProductRepository;
import com.pse.search.FuzzyMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ProductRepository productRepository;
    private final PriceEntryRepository priceEntryRepository;
    private final FuzzyMatcher fuzzyMatcher;
    private final RankingService rankingService;
    private final SearchResultCacheService cacheService;

    @Transactional(readOnly = true)
    public SearchResponse search(SearchRequest request) {
        String cacheKey = buildCacheKey(request);
        List<ProductDocument> cached = cacheService.get(cacheKey);
        if (cached != null) {
            return new SearchResponse(cached.size(), request.getPage(), request.getSize(), cached);
        }

        List<Product> products = loadCandidates(request);
        Map<String, PriceEntry> cheapestByProduct = cheapestPriceByProduct(products);

        List<ProductDocument> documents = new ArrayList<>();
        for (Product product : products) {
            PriceEntry entry = cheapestByProduct.get(product.getId());
            if (entry == null) {
                continue;
            }
            if (request.getMinPrice() != null && entry.getPrice().doubleValue() < request.getMinPrice()) {
                continue;
            }
            if (request.getMaxPrice() != null && entry.getPrice().doubleValue() > request.getMaxPrice()) {
                continue;
            }
            documents.add(ProductDocument.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .brand(product.getBrand())
                    .category(product.getCategory())
                    .description(product.getDescription())
                    .imageUrl(product.getImageUrl())
                    .price(entry.getPrice())
                    .availability(entry.getAvailability())
                    .sellerName(entry.getSeller().getName())
                    .build());
        }

        rankingService.rank(documents);

        int page = Math.max(request.getPage(), 0);
        int size = Math.min(Math.max(request.getSize(), 1), 100);
        int from = Math.min(page * size, documents.size());
        int to = Math.min(from + size, documents.size());
        List<ProductDocument> pageResults = documents.subList(from, to);

        cacheService.put(cacheKey, pageResults);

        return SearchResponse.builder()
                .total(documents.size())
                .page(page)
                .size(pageResults.size())
                .results(pageResults)
                .build();
    }

    private List<Product> loadCandidates(SearchRequest request) {
        List<Product> products;
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            products = productRepository.findAll();
        } else {
            String q = request.getQuery();
            List<Product> byName = productRepository.findByNameContainingIgnoreCase(q);
            List<Product> byBrand = productRepository.findByBrand(q);
            products = new ArrayList<>(byName);
            products.addAll(byBrand);
            if (products.isEmpty()) {
                products = productRepository.findAll().stream()
                        .filter(p -> fuzzyMatcher.matches(q, p.getName()) || fuzzyMatcher.matches(q, p.getBrand()))
                        .collect(Collectors.toList());
            }
        }
        return products.stream()
                .filter(p -> request.getCategory() == null || p.getCategory() == request.getCategory())
                .filter(p -> request.getBrand() == null || request.getBrand().equalsIgnoreCase(p.getBrand()))
                .toList();
    }

    private Map<String, PriceEntry> cheapestPriceByProduct(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        List<String> ids = products.stream().map(Product::getId).toList();
        List<PriceEntry> entries = priceEntryRepository.findByProductIdIn(ids);
        Map<String, List<PriceEntry>> grouped = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getProduct().getId()));

        Map<String, PriceEntry> result = new HashMap<>();
        for (var entryList : grouped.entrySet()) {
            result.put(entryList.getKey(), entryList.getValue().stream()
                    .min(Comparator
                            .comparing((PriceEntry e) -> e.getAvailability() == AvailabilityStatus.IN_STOCK ? 0 : 1)
                            .thenComparing(PriceEntry::getPrice))
                    .orElse(null));
        }
        return result;
    }

    private String buildCacheKey(SearchRequest request) {
        return request.getQuery() + "|" + request.getCategory() + "|" + request.getBrand()
                + "|" + request.getMinPrice() + "|" + request.getMaxPrice() + "|" + request.getSortBy()
                + "|" + request.getPage() + "|" + request.getSize();
    }
}
