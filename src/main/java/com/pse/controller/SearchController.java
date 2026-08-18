package com.pse.controller;

import com.pse.model.dto.SearchRequest;
import com.pse.model.dto.SearchResponse;
import com.pse.model.enums.ProductCategory;
import com.pse.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Full-text product search with ranking")
    public SearchResponse search(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) ProductCategory category,
                                 @RequestParam(required = false) String brand,
                                 @RequestParam(required = false) Double minPrice,
                                 @RequestParam(required = false) Double maxPrice,
                                 @RequestParam(required = false, defaultValue = "relevance") String sortBy,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return searchService.search(SearchRequest.builder()
                .query(q)
                .category(category)
                .brand(brand)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build());
    }
}
