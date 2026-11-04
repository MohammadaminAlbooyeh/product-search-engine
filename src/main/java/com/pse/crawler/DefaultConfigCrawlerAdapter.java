package com.pse.crawler;

import com.pse.crawler.config.SellerCrawlConfig;
import com.pse.model.entity.Seller;

import java.util.List;
import java.util.Locale;

/**
 * Base class for marketplace-specific adapters. Unlike the fixture adapters these
 * scrape the real site, but they ship with built-in selectors so a seller only
 * needs a URL. A seller-supplied {@code crawlConfig} always wins (handled by
 * {@link HttpSellerCrawlerAdapter}, which has higher precedence).
 */
public abstract class DefaultConfigCrawlerAdapter implements SellerCrawler {

    protected final HttpSellerCrawlerAdapter delegate;

    protected DefaultConfigCrawlerAdapter(HttpSellerCrawlerAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean supports(Seller seller) {
        return (seller.getCrawlConfig() == null || seller.getCrawlConfig().isBlank())
                && matches(seller.getUrl());
    }

    @Override
    public List<CrawlItem> crawl(Seller seller) {
        return delegate.crawlWithConfig(seller, defaultConfig(seller));
    }

    protected abstract boolean matches(String url);

    protected abstract SellerCrawlConfig defaultConfig(Seller seller);

    protected static boolean domainContains(String url, String domain) {
        return url != null && url.toLowerCase(Locale.ROOT).contains(domain);
    }
}
