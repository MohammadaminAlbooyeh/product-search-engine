package com.pse.service;

import com.pse.model.entity.PriceAlert;
import com.pse.model.entity.PriceEntry;
import com.pse.model.entity.Product;
import com.pse.model.entity.Seller;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.notification.EmailNotificationService;
import com.pse.repository.PriceAlertRepository;
import com.pse.repository.PriceEntryRepository;
import com.pse.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PriceAlertServiceTest {

    private final PriceAlertRepository alertRepo = mock(PriceAlertRepository.class);
    private final PriceEntryRepository priceRepo = mock(PriceEntryRepository.class);
    private final ProductRepository productRepo = mock(ProductRepository.class);
    private final EmailNotificationService email = mock(EmailNotificationService.class);
    private final PriceAlertService service =
            new PriceAlertService(alertRepo, priceRepo, productRepo, email);

    @Test
    void notifiesAndDeactivatesWhenPriceAtOrBelowTarget() {
        PriceEntry cheapest = PriceEntry.builder()
                .product(Product.builder().id("p1").build())
                .seller(Seller.builder().id(1L).name("digikala").build())
                .price(new BigDecimal("900"))
                .availability(AvailabilityStatus.IN_STOCK)
                .build();
        PriceAlert alert = PriceAlert.builder()
                .id(7L).productId("p1").email("user@example.com")
                .targetPrice(new BigDecimal("1000")).active(true).build();

        when(priceRepo.findTopByProductIdOrderByPriceAsc("p1")).thenReturn(Optional.of(cheapest));
        when(alertRepo.findByProductIdAndActiveTrue("p1")).thenReturn(List.of(alert));

        service.checkAndNotify("p1");

        verify(email).send(eq("user@example.com"), any(), any());
        verify(alertRepo).save(argThat(a -> !a.isActive()));
    }

    @Test
    void doesNothingWhenPriceAboveTarget() {
        PriceEntry cheapest = PriceEntry.builder()
                .product(Product.builder().id("p1").build())
                .seller(Seller.builder().id(1L).name("digikala").build())
                .price(new BigDecimal("1500"))
                .availability(AvailabilityStatus.IN_STOCK)
                .build();
        PriceAlert alert = PriceAlert.builder()
                .id(7L).productId("p1").email("user@example.com")
                .targetPrice(new BigDecimal("1000")).active(true).build();

        when(priceRepo.findTopByProductIdOrderByPriceAsc("p1")).thenReturn(Optional.of(cheapest));
        when(alertRepo.findByProductIdAndActiveTrue("p1")).thenReturn(List.of(alert));

        service.checkAndNotify("p1");

        verifyNoInteractions(email);
        verify(alertRepo, never()).save(any());
    }
}
