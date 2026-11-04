package com.pse.crawler;

import com.pse.crawler.config.SellerCrawlConfig;
import com.pse.model.entity.Seller;
import com.pse.model.enums.ProductCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Built-in eBay search-page adapter. Supply a seller with an {@code ebay.*} URL and
 * no {@code crawlConfig}; the selectors below are applied to eBay's static search
 * results. Override per seller with a custom {@code crawlConfig} when needed.
 */
@Component
@Order(60)
@Slf4j
public class EbayCrawlerAdapter extends DefaultConfigCrawlerAdapter {

    public EbayCrawlerAdapter(HttpSellerCrawlerAdapter delegate) {
        super(delegate);
    }

    @Override
    protected boolean matches(String url) {
        return domainContains(url, "ebay.");
    }

    @Override
    protected SellerCrawlConfig defaultConfig(Seller seller) {
        String base = seller.getUrl() == null || seller.getUrl().isBlank()
                ? "https://www.ebay.com" : trimTrailingSlash(seller.getUrl());
        log.info("Using built-in eBay crawl config for seller {}", seller.getName());
        return new SellerCrawlConfig(
                "html",
                null,
                base + "/sch/i.html?_nkw=deals&_pgn={page}",
                1, 2, null, true, ProductCategory.OTHER,
                "li.s-item",
                ".s-item__title",
                ".s-item__price",
                null,
                "a.s-item__link",
                null,
                "img.s-item__image-img",
                "src",
                null,
                null,
                null, null, null, null, null, null, null);
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
