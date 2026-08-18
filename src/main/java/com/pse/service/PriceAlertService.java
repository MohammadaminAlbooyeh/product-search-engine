package com.pse.service;

import com.pse.exception.ProductNotFoundException;
import com.pse.model.dto.PriceAlertRequest;
import com.pse.model.entity.PriceAlert;
import com.pse.model.entity.PriceEntry;
import com.pse.repository.PriceAlertRepository;
import com.pse.repository.PriceEntryRepository;
import com.pse.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final PriceEntryRepository priceEntryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public PriceAlert create(PriceAlertRequest request) {
        if (!productRepository.existsById(request.getProductId())) {
            throw new ProductNotFoundException(request.getProductId());
        }
        return priceAlertRepository.save(PriceAlert.builder()
                .productId(request.getProductId())
                .email(request.getEmail())
                .targetPrice(request.getTargetPrice())
                .active(true)
                .build());
    }

    public List<PriceAlert> listByEmail(String email) {
        return priceAlertRepository.findByEmail(email);
    }

    @Transactional
    public void deactivate(Long id) {
        priceAlertRepository.findById(id).ifPresent(alert -> {
            alert.setActive(false);
            priceAlertRepository.save(alert);
        });
    }

    @Transactional
    public void checkAndNotify(String productId) {
        priceEntryRepository.findTopByProductIdOrderByPriceAsc(productId).ifPresent(cheapest -> {
            for (PriceAlert alert : priceAlertRepository.findByProductIdAndActiveTrue(productId)) {
                if (cheapest.getPrice().compareTo(alert.getTargetPrice()) <= 0) {
                    sendEmail(alert, cheapest.getPrice());
                    alert.setActive(false);
                    priceAlertRepository.save(alert);
                }
            }
        });
    }

    private void sendEmail(PriceAlert alert, BigDecimal price) {
        log.info("Price alert triggered for {}: product {} now {}. Notifying {} (mock email)",
                alert.getEmail(), alert.getProductId(), price);
    }
}
