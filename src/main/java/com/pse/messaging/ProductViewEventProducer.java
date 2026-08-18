package com.pse.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductViewEventProducer {

    public static final String PRODUCT_VIEWED_TOPIC = "product.viewed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishProductViewed(String productId, String userId) {
        Map<String, Object> payload = Map.of(
                "productId", productId,
                "userId", userId != null ? userId : "anonymous",
                "timestamp", Instant.now().toString()
        );
        try {
            kafkaTemplate.send(PRODUCT_VIEWED_TOPIC, productId, payload);
        } catch (Exception e) {
            log.warn("Failed to publish product.viewed for {}: {}", productId, e.getMessage());
        }
    }
}
