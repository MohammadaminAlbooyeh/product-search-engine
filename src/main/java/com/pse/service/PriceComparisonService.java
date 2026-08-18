package com.pse.service;

import com.pse.model.dto.PriceOption;
import com.pse.model.dto.ProductComparisonDTO;
import com.pse.model.entity.PriceEntry;
import com.pse.model.entity.Product;
import com.pse.exception.ProductNotFoundException;
import com.pse.repository.PriceEntryRepository;
import com.pse.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceComparisonService {

    private final ProductRepository productRepository;
    private final PriceEntryRepository priceEntryRepository;

    @Transactional(readOnly = true)
    public ProductComparisonDTO compare(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        List<PriceEntry> entries = priceEntryRepository.findByProductIdOrderByPriceAsc(productId);
        List<PriceOption> prices = entries.stream()
                .map(e -> PriceOption.builder()
                        .sellerName(e.getSeller().getName())
                        .price(e.getPrice())
                        .url(e.getUrl())
                        .availability(e.getAvailability())
                        .build())
                .toList();

        BigDecimal cheapest = prices.stream()
                .map(PriceOption::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);

        BigDecimal average = prices.isEmpty() ? null
                : prices.stream().map(PriceOption::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);

        return ProductComparisonDTO.builder()
                .productId(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .cheapestPrice(cheapest)
                .averagePrice(average)
                .prices(prices)
                .build();
    }
}
