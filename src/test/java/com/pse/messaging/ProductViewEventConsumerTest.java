package com.pse.messaging;

import com.pse.service.ProductPopularityService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProductViewEventConsumerTest {

    private final ProductPopularityService popularityService = mock(ProductPopularityService.class);
    private final ProductViewEventConsumer consumer = new ProductViewEventConsumer(popularityService);

    @Test
    void recordsViewForProductInEvent() {
        consumer.onProductViewed(Map.of("productId", "p1", "userId", "u1"));

        verify(popularityService).recordView("p1");
    }

    @Test
    void ignoresEventWithoutProductId() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", "u1");

        consumer.onProductViewed(event);

        verify(popularityService, never()).recordView(org.mockito.ArgumentMatchers.anyString());
    }
}
