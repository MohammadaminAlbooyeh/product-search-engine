package com.pse.service;

import com.pse.model.dto.ProductComparisonDTO;
import com.pse.model.entity.PriceEntry;
import com.pse.model.entity.Product;
import com.pse.model.entity.Seller;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.model.enums.ProductCategory;
import com.pse.repository.PriceEntryRepository;
import com.pse.repository.ProductRepository;
import com.pse.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PriceComparisonServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final PriceEntryRepository priceEntryRepository = mock(PriceEntryRepository.class);
    private final PriceComparisonService service =
            new PriceComparisonService(productRepository, priceEntryRepository);

    @Test
    void compareReturnsCheapestAndAverage() {
        Product product = Product.builder()
                .id("p1").name("Laptop").category(ProductCategory.ELECTRONICS).build();
        Seller digi = Seller.builder().id(1L).name("digikala").build();
        Seller shop = Seller.builder().id(2L).name("techshop").build();

        when(productRepository.findById("p1")).thenReturn(Optional.of(product));
        when(priceEntryRepository.findByProductIdOrderByPriceAsc("p1")).thenReturn(List.of(
                entry(product, shop, "1000", AvailabilityStatus.IN_STOCK),
                entry(product, digi, "1200", AvailabilityStatus.IN_STOCK)));

        ProductComparisonDTO dto = service.compare("p1");

        assertThat(dto.getCheapestPrice()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(dto.getAveragePrice()).isEqualByComparingTo(new BigDecimal("1100.00"));
        assertThat(dto.getPrices()).hasSize(2);
        assertThat(dto.getPrices().get(0).getSellerName()).isEqualTo("techshop");
    }

    @Test
    void compareThrowsWhenProductMissing() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.compare("missing"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    private PriceEntry entry(Product product, Seller seller, String price, AvailabilityStatus availability) {
        return PriceEntry.builder()
                .product(product)
                .seller(seller)
                .price(new BigDecimal(price))
                .availability(availability)
                .url(seller.getName() + "/" + product.getId())
                .build();
    }
}