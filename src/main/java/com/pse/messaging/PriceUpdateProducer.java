package com.pse.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceUpdateProducer {

    public static final String PRICE_UPDATED_TOPIC = "price.updated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPriceUpdate(String productId, BigDecimal price, Long sellerId) {
        Map<String, Object> payload = Map.of(
                "productId", productId,
                "sellerId", sellerId,
                "price", price,
                "eventId", UUID.randomUUID().toString()
        );
        log.info("Publishing {} for product {}", PRICE_UPDATED_TOPIC, productId);
        kafkaTemplate.send(PRICE_UPDATED_TOPIC, productId, payload);
    }
}
