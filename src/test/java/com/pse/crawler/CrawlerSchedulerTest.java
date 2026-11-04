package com.pse.crawler;

import com.pse.messaging.PriceUpdateProducer;
import com.pse.model.entity.PriceEntry;
import com.pse.model.entity.Product;
import com.pse.model.entity.Seller;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.model.enums.ProductCategory;
import com.pse.observability.ApplicationMetrics;
import com.pse.repository.SellerRepository;
import com.pse.service.ProductAggregationService;
import com.pse.util.ProductIdGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CrawlerSchedulerTest {

    private final SellerRepository sellerRepository = mock(SellerRepository.class);
    private final CrawlerFactory crawlerFactory = mock(CrawlerFactory.class);
    private final ProductAggregationService aggregationService = mock(ProductAggregationService.class);
    private final ProductIdGenerator idGenerator = mock(ProductIdGenerator.class);
    private final PriceUpdateProducer priceUpdateProducer = mock(PriceUpdateProducer.class);
    private final ApplicationMetrics metrics = mock(ApplicationMetrics.class);

    private final CrawlerScheduler scheduler = new CrawlerScheduler(sellerRepository, crawlerFactory,
            aggregationService, idGenerator, priceUpdateProducer, metrics);

    @Test
    void crawlsEverySellerAndPublishesPriceUpdates() {
        Seller seller = Seller.builder().id(3L).name("techshop").url("https://techshop.example.com").build();
        SellerCrawler.CrawlItem item = new SellerCrawler.CrawlItem(null, "Laptop", "Acme",
                ProductCategory.ELECTRONICS, null, new BigDecimal("100"), "https://techshop.example.com/p/1",
                AvailabilityStatus.IN_STOCK);
        SellerCrawler crawler = mock(SellerCrawler.class);

        when(sellerRepository.findAll()).thenReturn(List.of(seller));
        when(crawlerFactory.forSeller(seller)).thenReturn(crawler);
        when(crawler.crawl(seller)).thenReturn(List.of(item));
        when(idGenerator.deterministic(anyString())).thenReturn("p-abc");
        when(aggregationService.upsertProduct(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PriceEntry entry = PriceEntry.builder().price(new BigDecimal("100"))
                .availability(AvailabilityStatus.IN_STOCK).build();
        when(aggregationService.upsertPrice(eq("p-abc"), eq("techshop"), any(), anyString(), any()))
                .thenReturn(entry);

        scheduler.crawlAll();

        verify(aggregationService).upsertProduct(argThat(p -> p.getId().equals("p-abc")
                && p.getName().equals("Laptop")));
        verify(priceUpdateProducer).publishPriceUpdate("p-abc", new BigDecimal("100"), 3L);
        verify(metrics).recordCrawl(eq("techshop"), eq(1), eq(true), any(Duration.class));
    }

    @Test
    void keepsGoingWhenOneSellerFails() {
        Seller seller = Seller.builder().id(3L).name("broken").build();
        SellerCrawler crawler = mock(SellerCrawler.class);

        when(sellerRepository.findAll()).thenReturn(List.of(seller));
        when(crawlerFactory.forSeller(seller)).thenReturn(crawler);
        when(crawler.crawl(seller)).thenThrow(new RuntimeException("boom"));

        assertThatCode(scheduler::crawlAll).doesNotThrowAnyException();

        verify(metrics).recordCrawl(eq("broken"), eq(0), eq(false), any(Duration.class));
        verifyNoInteractions(priceUpdateProducer);
    }

    @Test
    void skipsWhenNoSellersRegistered() {
        when(sellerRepository.findAll()).thenReturn(List.of());

        scheduler.crawlAll();

        verifyNoInteractions(crawlerFactory, aggregationService, priceUpdateProducer, metrics);
    }
}
