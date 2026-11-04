package com.pse.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductViewEventProducerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final ProductViewEventProducer producer = new ProductViewEventProducer(kafkaTemplate);

    @Test
    void publishesProductViewedWithGivenUser() {
        producer.publishProductViewed("p1", "u1");

        var captor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(kafkaTemplate).send(eq(ProductViewEventProducer.PRODUCT_VIEWED_TOPIC), eq("p1"), captor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) captor.getValue();
        assertThat(payload.get("productId")).isEqualTo("p1");
        assertThat(payload.get("userId")).isEqualTo("u1");
        assertThat(payload.get("timestamp")).isNotNull();
    }

    @Test
    void defaultsUserIdToAnonymousWhenNull() {
        producer.publishProductViewed("p1", null);

        var captor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(kafkaTemplate).send(eq(ProductViewEventProducer.PRODUCT_VIEWED_TOPIC), eq("p1"), captor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) captor.getValue();
        assertThat(payload.get("userId")).isEqualTo("anonymous");
    }

    @Test
    void swallowsKafkaFailureInsteadOfThrowing() {
        doThrow(new KafkaException("broker down"))
                .when(kafkaTemplate).send(any(String.class), any(String.class), any());

        producer.publishProductViewed("p1", "u1");
        // reaching here without an exception is the assertion
    }
}
