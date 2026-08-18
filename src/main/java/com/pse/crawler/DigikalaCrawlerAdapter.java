package com.pse.crawler;

import com.pse.model.entity.Seller;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.model.enums.ProductCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class DigikalaCrawlerAdapter implements SellerCrawler {

    private final Random random = new Random();

    @Override
    public boolean supports(Seller seller) {
        return seller.isDigikala();
    }

    @Override
    public List<SellerCrawler.CrawlItem> crawl(Seller seller) {
        log.info("Crawling Digikala for seller {}", seller.getName());
        return List.of(
                new SellerCrawler.CrawlItem("p-digi-laptop", "MacBook Air 13 M3", "Apple", ProductCategory.ELECTRONICS,
                        null, new BigDecimal("48990000"), seller.getUrl(), AvailabilityStatus.IN_STOCK),
                new SellerCrawler.CrawlItem("p-digi-phone", "Samsung Galaxy S24 Ultra", "Samsung", ProductCategory.ELECTRONICS,
                        null, new BigDecimal("55900000"), seller.getUrl(), AvailabilityStatus.IN_STOCK),
                new SellerCrawler.CrawlItem("p-digi-shoes", "Nike Air Max 270", "Nike", ProductCategory.SPORTS,
                        null, new BigDecimal("4290000"), seller.getUrl(), AvailabilityStatus.LIMITED),
                new SellerCrawler.CrawlItem("p-digi-tv", "Sony Bravia XR-65", "Sony", ProductCategory.ELECTRONICS,
                        null, new BigDecimal("89900000"), seller.getUrl(), AvailabilityStatus.OUT_OF_STOCK)
        );
    }
}
