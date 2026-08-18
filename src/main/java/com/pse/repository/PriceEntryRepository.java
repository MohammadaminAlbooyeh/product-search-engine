package com.pse.repository;

import com.pse.model.entity.PriceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PriceEntryRepository extends JpaRepository<PriceEntry, Long> {

    List<PriceEntry> findByProductIdOrderByPriceAsc(String productId);

    Optional<PriceEntry> findByProductIdAndSellerId(String productId, Long sellerId);

    Optional<PriceEntry> findTopByProductIdOrderByPriceAsc(String productId);

    List<PriceEntry> findByProductIdIn(java.util.Collection<String> productIds);

    long countByProductId(String productId);

    List<PriceEntry> findByPriceLessThanEqualAndAvailabilityIn(
            BigDecimal price, java.util.List<com.pse.model.enums.AvailabilityStatus> statuses);
}
