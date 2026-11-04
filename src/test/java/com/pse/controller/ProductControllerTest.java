package com.pse.controller;

import com.pse.exception.GlobalExceptionHandler;
import com.pse.exception.ProductNotFoundException;
import com.pse.messaging.ProductViewEventProducer;
import com.pse.model.dto.ProductComparisonDTO;
import com.pse.model.entity.PriceEntry;
import com.pse.model.entity.Product;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.model.enums.ProductCategory;
import com.pse.service.PriceComparisonService;
import com.pse.service.PriceHistoryService;
import com.pse.service.ProductAggregationService;
import com.pse.util.ProductIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductControllerTest {

    private final ProductAggregationService aggregation = mock(ProductAggregationService.class);
    private final PriceComparisonService comparison = mock(PriceComparisonService.class);
    private final PriceHistoryService history = mock(PriceHistoryService.class);
    private final ProductViewEventProducer viewProducer = mock(ProductViewEventProducer.class);
    private final ProductIdGenerator idGenerator = mock(ProductIdGenerator.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(aggregation, comparison, history,
                viewProducer, idGenerator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createProductReturns201AndGeneratedId() throws Exception {
        when(idGenerator.generate()).thenReturn("p-gen");
        when(aggregation.upsertProduct(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Laptop","brand":"Acme","category":"ELECTRONICS"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("p-gen"))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.category").value("ELECTRONICS"));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verifyNoInteractions(aggregation);
    }

    @Test
    void malformedOrUnknownEnumBodyReturns400Not500() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Laptop\",\"category\":\"NOT_A_CATEGORY\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPublishesViewAndReturnsComparisonAndHistory() throws Exception {
        ProductComparisonDTO dto = ProductComparisonDTO.builder()
                .productId("p1").name("Laptop").cheapestPrice(new BigDecimal("10"))
                .prices(List.of()).build();
        when(comparison.compare("p1")).thenReturn(dto);
        when(history.historyFor("p1")).thenReturn(List.of());

        mockMvc.perform(get("/api/products/p1").header("X-User-Id", "u-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.productId").value("p1"))
                .andExpect(jsonPath("$.priceHistory").isArray());

        verify(viewProducer).publishProductViewed("p1", "u-1");
    }

    @Test
    void missingProductReturns404() throws Exception {
        when(comparison.compare("nope")).thenThrow(new ProductNotFoundException("nope"));

        mockMvc.perform(get("/api/products/nope/compare"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found: nope"));
    }

    @Test
    void upsertPriceReturnsEntry() throws Exception {
        PriceEntry entry = PriceEntry.builder().price(new BigDecimal("42"))
                .availability(AvailabilityStatus.IN_STOCK).build();
        when(aggregation.upsertPrice(eq("p1"), eq("shop"), any(), any(), any())).thenReturn(entry);

        mockMvc.perform(post("/api/products/p1/price")
                        .param("seller", "shop")
                        .param("price", "42")
                        .param("availability", "IN_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(42))
                .andExpect(jsonPath("$.availability").value("IN_STOCK"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/products/p1"))
                .andExpect(status().isNoContent());

        verify(aggregation).deleteProduct("p1");
    }
}
