package com.pse.controller;

import com.pse.model.dto.SearchRequest;
import com.pse.model.dto.SearchResponse;
import com.pse.model.enums.ProductCategory;
import com.pse.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Full-text product search with availability, price and popularity ranking")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Full-text product search with ranking",
            description = "Ranking combines availability (highest weight), price and popularity from product.viewed events.")
    @ApiResponse(responseCode = "200", description = "Search results")
    public SearchResponse search(
            @Parameter(description = "Free-text query; fuzzy-matched against name and brand", example = "macbook")
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String brand,
            @Parameter(description = "Minimum cheapest-seller price", example = "1000000")
            @RequestParam(required = false) Double minPrice,
            @Parameter(description = "Maximum cheapest-seller price", example = "60000000")
            @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "relevance, price_asc or price_desc", example = "relevance")
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
