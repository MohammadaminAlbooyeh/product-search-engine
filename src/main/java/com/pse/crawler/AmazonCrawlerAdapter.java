package com.pse.crawler;

import com.pse.crawler.config.SellerCrawlConfig;
import com.pse.model.entity.Seller;
import com.pse.model.enums.ProductCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Built-in Amazon search-page adapter. Supply a seller with an
 * {@code amazon.*} URL and no {@code crawlConfig}; the selectors below are applied
 * to Amazon's static search results. Override per seller with a custom
 * {@code crawlConfig} if Amazon changes its markup or a different locale is used.
 */
@Component
@Order(50)
@Slf4j
public class AmazonCrawlerAdapter extends DefaultConfigCrawlerAdapter {

    public AmazonCrawlerAdapter(HttpSellerCrawlerAdapter delegate) {
        super(delegate);
    }

    @Override
    protected boolean matches(String url) {
        return domainContains(url, "amazon.");
    }

    @Override
    protected SellerCrawlConfig defaultConfig(Seller seller) {
        String base = seller.getUrl() == null || seller.getUrl().isBlank()
                ? "https://www.amazon.com" : trimTrailingSlash(seller.getUrl());
        log.info("Using built-in Amazon crawl config for seller {}", seller.getName());
        return new SellerCrawlConfig(
                "html",
                null,
                base + "/s?k=deals&page={page}",
                1, 2, null, true, ProductCategory.OTHER,
                "div[data-component-type='s-search-result']",
                "h2 a span",
                ".a-price .a-offscreen",
                null,
                "h2 a",
                null,
                "img.s-image",
                null,
                null,
                null,
                null, null, null, null, null, null, null);
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
