package com.pse.service;

import com.pse.model.document.ProductDocument;
import com.pse.model.enums.AvailabilityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RankingService {

    @Nullable
    private final ProductPopularityService popularityService;
    private final double popularityWeight;

    /** Used by unit tests and as a no-op default (no popularity signal). */
    public RankingService() {
        this(null, 0.0);
    }

    @Autowired
    public RankingService(@Nullable ProductPopularityService popularityService,
                          @Value("${ranking.popularity.weight:2.0}") double popularityWeight) {
        this.popularityService = popularityService;
        this.popularityWeight = popularityWeight;
    }

    public double score(ProductDocument doc) {
        double availabilityScore;
        switch (doc.getAvailability() == null ? AvailabilityStatus.OUT_OF_STOCK : doc.getAvailability()) {
            case IN_STOCK -> availabilityScore = 1.0;
            case LIMITED -> availabilityScore = 0.7;
            case DISCONTINUED -> availabilityScore = 0.2;
            default -> availabilityScore = 0.5;
        }
        double priceScore = 0.0;
        if (doc.getPrice() != null) {
            double price = doc.getPrice().doubleValue();
            if (price > 0) {
                priceScore = 1.0 / (1.0 + price / 1_000_000);
            }
        }
        double popularityScore = 0.0;
        if (popularityService != null && doc.getId() != null) {
            popularityScore = popularityService.popularityBoost(doc.getId()) * popularityWeight;
        }
        return availabilityScore * 10 + priceScore * 3 + popularityScore + doc.getScore();
    }

    public List<ProductDocument> rank(List<ProductDocument> documents) {
        List<ProductDocument> ranked = new ArrayList<>(documents);
        ranked.forEach(doc -> doc.setScore(score(doc)));
        ranked.sort(Comparator.comparingDouble(ProductDocument::getScore).reversed());
        return ranked;
    }
}
