package com.pse.service;

import com.pse.model.document.ProductDocument;
import com.pse.model.enums.AvailabilityStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankingServicePopularityTest {

    @Test
    void popularProductRanksAboveEquivalentUnpopularOne() {
        ProductPopularityService popularity = new ProductPopularityService(null);
        for (int i = 0; i < 50; i++) {
            popularity.recordView("p-popular");
        }
        RankingService ranking = new RankingService(popularity, 2.0);

        ProductDocument popular = doc("p-popular");
        ProductDocument cold = doc("p-cold");

        List<ProductDocument> ranked = ranking.rank(List.of(cold, popular));

        assertThat(ranked.get(0).getId()).isEqualTo("p-popular");
    }

    @Test
    void zeroWeightDisablesPopularityInfluence() {
        ProductPopularityService popularity = new ProductPopularityService(null);
        for (int i = 0; i < 1000; i++) {
            popularity.recordView("p1");
        }
        RankingService ranking = new RankingService(popularity, 0.0);
        RankingService baseline = new RankingService();

        assertThat(ranking.score(doc("p1"))).isEqualTo(baseline.score(doc("p1")));
    }

    private ProductDocument doc(String id) {
        return ProductDocument.builder()
                .id(id)
                .name("Laptop " + id)
                .price(new BigDecimal("1000000"))
                .availability(AvailabilityStatus.IN_STOCK)
                .build();
    }
}
