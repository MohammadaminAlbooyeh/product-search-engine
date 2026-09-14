package com.pse.crawler;

import com.pse.exception.CrawlerFailedException;
import com.pse.model.entity.Seller;
import com.pse.model.enums.AvailabilityStatus;

import java.math.BigDecimal;
import java.util.List;

public interface SellerCrawler {

    boolean supports(Seller seller);

    List<CrawlItem> crawl(Seller seller) throws CrawlerFailedException;

    record CrawlItem(String productId, String name, String brand,
                     com.pse.model.enums.ProductCategory category, String imageUrl,
                     BigDecimal price, String url, AvailabilityStatus availability) {
    }
}
