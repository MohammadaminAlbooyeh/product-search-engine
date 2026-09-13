package com.pse.crawler;

import com.pse.messaging.PriceUpdateProducer;
import com.pse.model.entity.PriceEntry;
import com.pse.model.entity.Product;
import com.pse.model.entity.Seller;
import com.pse.repository.SellerRepository;
import com.pse.service.ProductAggregationService;
import com.pse.util.ProductIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlerScheduler {

    private final SellerRepository sellerRepository;
    private final CrawlerFactory crawlerFactory;
    private final ProductAggregationService aggregationService;
    private final ProductIdGenerator productIdGenerator;
    private final PriceUpdateProducer priceUpdateProducer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${crawler.interval-ms:3600000}", initialDelayString = "${crawler.initial-delay-ms:30000}")
    public void crawlAll() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            List<Seller> sellers = sellerRepository.findAll();
            if (sellers.isEmpty()) {
                log.info("No sellers registered, skipping crawl");
                return;
            }
            for (Seller seller : sellers) {
                try {
                    SellerCrawler crawler = crawlerFactory.forSeller(seller);
                    for (SellerCrawler.CrawlItem item : crawler.crawl(seller)) {
                        Product product = Product.builder()
                                .id(resolveProductId(item, seller))
                                .name(item.name())
                                .brand(item.brand())
                                .category(item.category())
                                .imageUrl(item.imageUrl())
                                .build();
                        aggregationService.upsertProduct(product);
                        PriceEntry entry = aggregationService.upsertPrice(
                                product.getId(), seller.getName(), item.price(), item.url(), item.availability());
                        priceUpdateProducer.publishPriceUpdate(product.getId(), entry.getPrice(), seller.getId());
                    }
                } catch (Exception e) {
                    log.error("Crawl failed for seller {}: {}", seller.getName(), e.getMessage());
                }
            }
        } finally {
            running.set(false);
        }
    }

    private String resolveProductId(SellerCrawler.CrawlItem item, Seller seller) {
        if (item.productId() != null && !item.productId().isBlank()) {
            return item.productId();
        }
        String seed = item.url() != null && !item.url().isBlank()
                ? item.url()
                : seller.getName() + "|" + item.name();
        return productIdGenerator.deterministic(seed);
    }
}
