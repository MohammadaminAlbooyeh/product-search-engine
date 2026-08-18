package com.pse.crawler;

import com.pse.model.entity.Seller;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CrawlerFactory {

    private final List<SellerCrawler> crawlers;

    public CrawlerFactory(List<SellerCrawler> crawlers) {
        this.crawlers = crawlers;
    }

    public SellerCrawler forSeller(Seller seller) {
        return crawlers.stream()
                .filter(c -> c.supports(seller))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No crawler for seller " + seller.getName()));
    }
}
