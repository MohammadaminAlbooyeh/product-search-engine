package com.pse.service;

import com.pse.model.document.ProductDocument;
import com.pse.model.enums.AvailabilityStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankingServiceTest {

    private final RankingService rankingService = new RankingService();

    @Test
    void inStockRanksAboveOutOfStock() {
        ProductDocument inStock = doc("p1", "1000000", AvailabilityStatus.IN_STOCK);
        ProductDocument out = doc("p2", "1000000", AvailabilityStatus.OUT_OF_STOCK);

        List<ProductDocument> ranked = rankingService.rank(List.of(out, inStock));

        assertThat(ranked.get(0).getId()).isEqualTo("p1");
    }

    @Test
    void cheaperInStockRanksHigher() {
        ProductDocument expensive = doc("p1", "90000000", AvailabilityStatus.IN_STOCK);
        ProductDocument cheap = doc("p2", "500000", AvailabilityStatus.IN_STOCK);

        List<ProductDocument> ranked = rankingService.rank(List.of(expensive, cheap));

        assertThat(ranked.get(0).getId()).isEqualTo("p2");
    }

    @Test
    void scoreReflectsAvailability() {
        double inStock = rankingService.score(doc("p1", "1000000", AvailabilityStatus.IN_STOCK));
        double discontinued = rankingService.score(doc("p2", "1000000", AvailabilityStatus.DISCONTINUED));
        assertThat(inStock).isGreaterThan(discontinued);
    }

    private ProductDocument doc(String id, String price, AvailabilityStatus status) {
        return ProductDocument.builder()
                .id(id)
                .name("Laptop " + id)
                .price(new BigDecimal(price))
                .availability(status)
                .build();
    }
}