package com.pse.crawler;

import com.pse.model.entity.Seller;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.model.enums.ProductCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
public class GenericSellerCrawlerAdapter implements SellerCrawler {

    @Override
    public boolean supports(Seller seller) {
        return !seller.isDigikala();
    }

    @Override
    public List<SellerCrawler.CrawlItem> crawl(Seller seller) {
        log.info("Crawling generic seller {}", seller.getName());
        return List.of(
                new SellerCrawler.CrawlItem("p-gen-laptop", "MacBook Air 13 M3", "Apple", ProductCategory.ELECTRONICS,
                        null, new BigDecimal("51200000"), seller.getUrl(), AvailabilityStatus.IN_STOCK),
                new SellerCrawler.CrawlItem("p-gen-phone", "Samsung Galaxy S24 Ultra", "Samsung", ProductCategory.ELECTRONICS,
                        null, new BigDecimal("54700000"), seller.getUrl(), AvailabilityStatus.IN_STOCK),
                new SellerCrawler.CrawlItem("p-gen-keyboard", "Logitech MX Keys", "Logitech", ProductCategory.ELECTRONICS,
                        null, new BigDecimal("3150000"), seller.getUrl(), AvailabilityStatus.IN_STOCK)
        );
    }
}
