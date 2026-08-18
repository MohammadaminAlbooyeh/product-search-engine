package com.pse.service;

import com.pse.model.entity.PriceEntry;
import com.pse.model.entity.PriceHistory;
import com.pse.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;

    public void record(PriceEntry entry) {
        priceHistoryRepository.save(PriceHistory.builder()
                .productId(entry.getProduct().getId())
                .sellerId(entry.getSeller().getId())
                .price(entry.getPrice())
                .build());
    }

    public List<PriceHistory> historyFor(String productId) {
        return priceHistoryRepository.findByProductIdOrderByRecordedAtDesc(productId);
    }
}
