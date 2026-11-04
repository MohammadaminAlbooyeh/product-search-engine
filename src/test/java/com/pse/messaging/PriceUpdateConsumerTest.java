package com.pse.messaging;

import com.pse.service.PriceAlertService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PriceUpdateConsumerTest {

    private final PriceAlertService priceAlertService = mock(PriceAlertService.class);
    private final PriceUpdateConsumer consumer = new PriceUpdateConsumer(priceAlertService);

    @Test
    void checksAlertsForProductInEvent() {
        consumer.onPriceUpdated(Map.of("productId", "p1", "price", "199.99"));

        verify(priceAlertService).checkAndNotify("p1");
    }
}
