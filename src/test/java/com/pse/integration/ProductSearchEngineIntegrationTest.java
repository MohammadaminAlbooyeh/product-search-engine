package com.pse.integration;

import com.pse.model.dto.ProductComparisonDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

class ProductSearchEngineIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @LocalServerPort
    int port;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders adminJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth("admin", "admin");
        return headers;
    }

    @Test
    void mutatingEndpointsRequireAuthentication() {
        ResponseEntity<String> response = rest.postForEntity(url("/api/products"),
                new HttpEntity<>("{\"name\":\"x\",\"category\":\"OTHER\"}", jsonHeaders()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void endToEndCreateSearchCompareAndAlert() {
        // create product (authenticated)
        String productBody = """
                {"id":"p-int-1","name":"Integration Laptop","brand":"Acme","category":"ELECTRONICS"}
                """;
        ResponseEntity<Map> created = rest.postForEntity(url("/api/products"),
                new HttpEntity<>(productBody, adminJsonHeaders()), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // add two seller prices
        rest.exchange(url("/api/products/p-int-1/price?seller=digikala&price=48000000&availability=IN_STOCK"),
                HttpMethod.POST, new HttpEntity<>(adminJsonHeaders()), String.class);
        rest.exchange(url("/api/products/p-int-1/price?seller=techshop&price=45000000&availability=IN_STOCK"),
                HttpMethod.POST, new HttpEntity<>(adminJsonHeaders()), String.class);

        // public search finds it (relational path, no auth)
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<Map> search = rest.getForEntity(url("/api/search?q=Integration"), Map.class);
            assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(((Number) search.getBody().get("total")).intValue()).isGreaterThanOrEqualTo(1);
        });

        // compare returns the cheapest seller price
        ResponseEntity<ProductComparisonDTO> compare =
                rest.getForEntity(url("/api/products/p-int-1/compare"), ProductComparisonDTO.class);
        assertThat(compare.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(compare.getBody().getCheapestPrice()).isEqualByComparingTo("45000000");

        // create a price alert
        String alertBody = """
                {"productId":"p-int-1","email":"buyer@example.com","targetPrice":40000000}
                """;
        ResponseEntity<Map> alert = rest.postForEntity(url("/api/price-alerts"),
                new HttpEntity<>(alertBody, adminJsonHeaders()), Map.class);
        assertThat(alert.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
