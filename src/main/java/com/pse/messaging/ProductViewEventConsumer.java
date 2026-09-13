package com.pse.messaging;

import com.pse.service.ProductPopularityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes {@code product.viewed} events and feeds them into the popularity signal
 * used by ranking.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductViewEventConsumer {

    private final ProductPopularityService popularityService;

    @KafkaListener(topics = ProductViewEventProducer.PRODUCT_VIEWED_TOPIC, groupId = "product-search-engine")
    public void onProductViewed(Map<String, Object> event) {
        Object productId = event.get("productId");
        if (productId == null) {
            return;
        }
        log.debug("product.viewed consumed for {}", productId);
        popularityService.recordView(String.valueOf(productId));
    }
}
