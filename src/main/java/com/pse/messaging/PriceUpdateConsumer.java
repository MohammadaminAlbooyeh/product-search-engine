package com.pse.messaging;

import com.pse.service.PriceAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceUpdateConsumer {

    private final PriceAlertService priceAlertService;

    @KafkaListener(topics = PriceUpdateProducer.PRICE_UPDATED_TOPIC, groupId = "product-search-engine")
    public void onPriceUpdated(Map<String, Object> event) {
        String productId = String.valueOf(event.get("productId"));
        log.info("Price update consumed for product {}", productId);
        priceAlertService.checkAndNotify(productId);
    }
}
