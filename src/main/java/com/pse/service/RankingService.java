package com.pse.service;

import com.pse.model.document.ProductDocument;
import com.pse.model.enums.AvailabilityStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RankingService {

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
        double base = availabilityScore * 10 + priceScore * 3 + doc.getScore();
        return base;
    }

    public List<ProductDocument> rank(List<ProductDocument> documents) {
        List<ProductDocument> ranked = new ArrayList<>(documents);
        ranked.forEach(doc -> doc.setScore(score(doc)));
        ranked.sort(Comparator.comparingDouble(ProductDocument::getScore).reversed());
        return ranked;
    }
}
