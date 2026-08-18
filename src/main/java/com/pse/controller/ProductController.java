package com.pse.controller;

import com.pse.messaging.ProductViewEventProducer;
import com.pse.model.dto.ProductComparisonDTO;
import com.pse.model.entity.PriceEntry;
import com.pse.model.entity.Product;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.model.enums.ProductCategory;
import com.pse.service.PriceComparisonService;
import com.pse.service.ProductAggregationService;
import com.pse.service.PriceHistoryService;
import com.pse.service.SearchService;
import com.pse.util.ProductIdGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

    private final ProductAggregationService aggregationService;
    private final PriceComparisonService comparisonService;
    private final PriceHistoryService priceHistoryService;
    private final ProductViewEventProducer viewEventProducer;
    private final ProductIdGenerator productIdGenerator;

    @PostMapping
    @Operation(summary = "Register a product")
    public ResponseEntity<Product> create(@RequestBody Map<String, Object> body) {
        Product product = Product.builder()
                .id(String.valueOf(body.getOrDefault("id", productIdGenerator.generate())))
                .name(String.valueOf(body.get("name")))
                .brand((String) body.get("brand"))
                .category(ProductCategory.valueOf(String.valueOf(body.getOrDefault("category", "OTHER"))))
                .imageUrl((String) body.get("imageUrl"))
                .description((String) body.get("description"))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(aggregationService.upsertProduct(product));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product detail and publish product.viewed")
    public ResponseEntity<Map<String, Object>> get(
            @PathVariable String productId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        viewEventProducer.publishProductViewed(productId, userId);
        return ResponseEntity.ok(Map.of(
                "product", comparisonService.compare(productId),
                "priceHistory", priceHistoryService.historyFor(productId)));
    }

    @GetMapping("/{productId}/compare")
    @Operation(summary = "Compare prices across sellers")
    public ResponseEntity<ProductComparisonDTO> compare(@PathVariable String productId) {
        return ResponseEntity.ok(comparisonService.compare(productId));
    }

    @PostMapping("/{productId}/price")
    @Operation(summary = "Upsert a price entry for a seller")
    public ResponseEntity<PriceEntry> upsertPrice(
            @PathVariable String productId,
            @RequestParam String seller,
            @RequestParam @DecimalMin("0.01") BigDecimal price,
            @RequestParam(defaultValue = "IN_STOCK") AvailabilityStatus availability,
            @RequestParam(required = false) String url) {
        return ResponseEntity.ok(aggregationService.upsertPrice(
                productId, seller, price, url, availability));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete a product and its prices")
    public ResponseEntity<Void> delete(@PathVariable String productId) {
        aggregationService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
