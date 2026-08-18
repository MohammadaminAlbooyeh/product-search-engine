package com.pse.service;

import com.pse.cache.SearchResultCacheService;
import com.pse.model.dto.SearchRequest;
import com.pse.model.dto.SearchResponse;
import com.pse.model.entity.PriceEntry;
import com.pse.model.entity.Product;
import com.pse.model.entity.Seller;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.model.enums.ProductCategory;
import com.pse.repository.PriceEntryRepository;
import com.pse.repository.ProductRepository;
import com.pse.search.FuzzyMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final PriceEntryRepository priceEntryRepository = mock(PriceEntryRepository.class);
    private final SearchResultCacheService cacheService = mock(SearchResultCacheService.class);
    private final SearchService searchService =
            new SearchService(productRepository, priceEntryRepository, new FuzzyMatcher(),
                    new RankingService(), cacheService);

    private Product laptop;
    private Product shoes;

    @BeforeEach
    void setUp() {
        laptop = Product.builder()
                .id("p1").name("MacBook Air M3").brand("Apple")
                .category(ProductCategory.ELECTRONICS).build();
        shoes = Product.builder()
                .id("p2").name("Nike Air Max").brand("Nike")
                .category(ProductCategory.SPORTS).build();

        Seller digi = Seller.builder().id(1L).name("digikala").build();
        PriceEntry laptopEntry = PriceEntry.builder()
                .product(laptop).seller(digi).price(new BigDecimal("48990000"))
                .availability(AvailabilityStatus.IN_STOCK).build();
        PriceEntry shoesEntry = PriceEntry.builder()
                .product(shoes).seller(digi).price(new BigDecimal("4290000"))
                .availability(AvailabilityStatus.OUT_OF_STOCK).build();

        when(productRepository.findByNameContainingIgnoreCase("laptop"))
                .thenReturn(List.of(laptop));
        when(productRepository.findByBrand("Apple")).thenReturn(List.of(laptop));
        when(productRepository.findAll()).thenReturn(List.of(laptop, shoes));
        when(priceEntryRepository.findByProductIdIn(any()))
                .thenReturn(List.of(laptopEntry, shoesEntry));
        when(cacheService.get(any())).thenReturn(null);
    }

    @Test
    void searchByQueryFindsMatchingProducts() {
        SearchResponse response = searchService.search(SearchRequest.builder()
                .query("laptop").page(0).size(10).build());
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getResults().get(0).getId()).isEqualTo("p1");
    }

    @Test
    void searchWithPriceRangeFiltersResults() {
        SearchResponse response = searchService.search(SearchRequest.builder()
                .query("").minPrice(0.0).maxPrice(10_000_000.0).page(0).size(10).build());
        assertThat(response.getResults()).extracting(r -> r.getId())
                .containsExactly("p2");
    }

    @Test
    void inStockRanksFirst() {
        SearchResponse response = searchService.search(SearchRequest.builder()
                .query("").page(0).size(10).build());
        assertThat(response.getResults().get(0).getId()).isEqualTo("p1");
    }
}