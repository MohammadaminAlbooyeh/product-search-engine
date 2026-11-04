package com.pse.controller;

import com.pse.messaging.ProductViewEventProducer;
import com.pse.model.dto.CreateProductRequest;
import com.pse.model.dto.ProductComparisonDTO;
import com.pse.model.entity.PriceEntry;
import com.pse.model.entity.Product;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.model.enums.ProductCategory;
import com.pse.service.PriceComparisonService;
import com.pse.service.PriceHistoryService;
import com.pse.service.ProductAggregationService;
import com.pse.util.ProductIdGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Products", description = "Product catalogue, price comparison and price history")
public class ProductController {

    private final ProductAggregationService aggregationService;
    private final PriceComparisonService comparisonService;
    private final PriceHistoryService priceHistoryService;
    private final ProductViewEventProducer viewEventProducer;
    private final ProductIdGenerator productIdGenerator;

    @PostMapping
    @Operation(summary = "Register a product", description = "Creates or upserts the product and indexes it for search.")
    @SecurityRequirement(name = "basicAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product registered"),
            @ApiResponse(responseCode = "400", description = "Invalid payload"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid admin credentials")
    })
    public ResponseEntity<Product> create(@Valid @RequestBody CreateProductRequest request) {
        Product product = Product.builder()
                .id(request.getId() != null && !request.getId().isBlank()
                        ? request.getId() : productIdGenerator.generate())
                .name(request.getName())
                .brand(request.getBrand())
                .category(request.getCategory() != null ? request.getCategory() : ProductCategory.OTHER)
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(aggregationService.upsertProduct(product));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product detail and price history",
            description = "Also publishes a product.viewed event that feeds popularity-based ranking.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Map<String, Object>> get(
            @Parameter(description = "Product id", example = "p-digi-laptop")
            @PathVariable String productId,
            @Parameter(description = "Optional user id attached to the view event")
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        viewEventProducer.publishProductViewed(productId, userId);
        return ResponseEntity.ok(Map.of(
                "product", comparisonService.compare(productId),
                "priceHistory", priceHistoryService.historyFor(productId)));
    }

    @GetMapping("/{productId}/compare")
    @Operation(summary = "Compare prices across sellers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comparison returned"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductComparisonDTO> compare(@Parameter(example = "p-digi-laptop")
                                                        @PathVariable String productId) {
        return ResponseEntity.ok(comparisonService.compare(productId));
    }

    @PostMapping("/{productId}/price")
    @Operation(summary = "Upsert a price entry for a seller")
    @SecurityRequirement(name = "basicAuth")
    @ApiResponse(responseCode = "200", description = "Price stored and product re-indexed")
    public ResponseEntity<PriceEntry> upsertPrice(
            @Parameter(example = "p-digi-laptop") @PathVariable String productId,
            @Parameter(description = "Seller name", example = "digikala") @RequestParam String seller,
            @Parameter(description = "Price in the smallest currency unit", example = "48990000")
            @RequestParam @DecimalMin("0.01") BigDecimal price,
            @RequestParam(defaultValue = "IN_STOCK") AvailabilityStatus availability,
            @RequestParam(required = false) String url) {
        return ResponseEntity.ok(aggregationService.upsertPrice(
                productId, seller, price, url, availability));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete a product and its prices")
    @SecurityRequirement(name = "basicAuth")
    @ApiResponse(responseCode = "204", description = "Product deleted")
    public ResponseEntity<Void> delete(@Parameter(example = "p-digi-laptop") @PathVariable String productId) {
        aggregationService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
