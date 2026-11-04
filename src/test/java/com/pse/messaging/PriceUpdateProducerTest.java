package com.pse.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PriceUpdateProducerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

    @Test
    void publishesPriceUpdateWithExpectedKeyAndPayload() {
        PriceUpdateProducer producer = new PriceUpdateProducer(kafkaTemplate);

        producer.publishPriceUpdate("p1", new BigDecimal("1999.99"), 42L);

        var payloadCaptor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(kafkaTemplate).send(eq(PriceUpdateProducer.PRICE_UPDATED_TOPIC), eq("p1"), payloadCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload.get("productId")).isEqualTo("p1");
        assertThat(payload.get("sellerId")).isEqualTo(42L);
        assertThat(payload.get("price")).isEqualTo(new BigDecimal("1999.99"));
        assertThat(payload.get("eventId")).isNotNull();
    }
}
