package com.pse.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pse.crawler.http.CrawlRateLimiter;
import com.pse.crawler.http.HttpFetcher;
import com.pse.crawler.http.RobotsTxtService;
import com.pse.model.entity.Seller;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.util.PriceNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HttpSellerCrawlerAdapterTest {

    private HttpFetcher httpFetcher;
    private RobotsTxtService robotsTxtService;
    private CrawlRateLimiter rateLimiter;
    private HttpSellerCrawlerAdapter adapter;

    @BeforeEach
    void setUp() {
        httpFetcher = mock(HttpFetcher.class);
        robotsTxtService = mock(RobotsTxtService.class);
        rateLimiter = mock(CrawlRateLimiter.class);
        when(httpFetcher.defaultUserAgent()).thenReturn("test-bot/1.0");
        when(robotsTxtService.isAllowed(anyString(), anyString())).thenReturn(true);
        adapter = new HttpSellerCrawlerAdapter(httpFetcher, robotsTxtService, rateLimiter,
                new PriceNormalizer(), new ObjectMapper(), 0L);
    }

    private Seller sellerWithConfig(String config) {
        return Seller.builder().name("techshop").url("https://shop.example.com").crawlConfig(config).build();
    }

    @Test
    void supportsOnlyWhenCrawlConfigPresent() {
        assertThat(adapter.supports(sellerWithConfig("{}"))).isTrue();
        assertThat(adapter.supports(Seller.builder().name("x").build())).isFalse();
        assertThat(adapter.supports(Seller.builder().name("x").crawlConfig("  ").build())).isFalse();
    }

    @Test
    void parsesHtmlListingAndStopsWhenPageEmpty() {
        String config = """
                {
                  "mode": "html",
                  "listingUrlTemplate": "https://shop.example.com/list?page={page}",
                  "startPage": 1,
                  "maxPages": 5,
                  "defaultCategory": "ELECTRONICS",
                  "itemSelector": "div.card",
                  "nameSelector": ".title",
                  "priceSelector": ".price",
                  "brandSelector": ".brand",
                  "linkSelector": "a.link",
                  "imageSelector": "img"
                }
                """;
        String page1 = """
                <html><body>
                  <div class="card">
                    <a class="link" href="/p/1">go</a>
                    <span class="title">Laptop Pro 14</span>
                    <span class="brand">Acme</span>
                    <span class="price">$45,900,000</span>
                    <img src="/img/1.jpg"/>
                  </div>
                  <div class="card">
                    <a class="link" href="/p/2">go</a>
                    <span class="title">Phone X</span>
                    <span class="brand">Zeta</span>
                    <span class="price">21000000</span>
                  </div>
                </body></html>
                """;
        when(httpFetcher.get(contains("page=1"), anyString()))
                .thenReturn(new HttpFetcher.FetchResult(200, page1, "https://shop.example.com/list?page=1"));
        when(httpFetcher.get(contains("page=2"), anyString()))
                .thenReturn(new HttpFetcher.FetchResult(200, "<html><body></body></html>", "x"));

        List<SellerCrawler.CrawlItem> items = adapter.crawl(sellerWithConfig(config));

        assertThat(items).hasSize(2);
        SellerCrawler.CrawlItem first = items.get(0);
        assertThat(first.name()).isEqualTo("Laptop Pro 14");
        assertThat(first.brand()).isEqualTo("Acme");
        assertThat(first.price()).isEqualByComparingTo(new BigDecimal("45900000"));
        assertThat(first.url()).isEqualTo("https://shop.example.com/p/1");
        assertThat(first.availability()).isEqualTo(AvailabilityStatus.IN_STOCK);
        verify(httpFetcher, never()).get(contains("page=3"), anyString());
        verify(rateLimiter, atLeastOnce()).acquire(eq("shop.example.com"), anyLong());
    }

    @Test
    void parsesJsonListing() {
        String config = """
                {
                  "mode": "json",
                  "listingUrlTemplate": "https://api.example.com/products?page={page}",
                  "maxPages": 1,
                  "defaultCategory": "HOME",
                  "itemsPointer": "/data/items",
                  "namePointer": "/title",
                  "pricePointer": "/price/amount",
                  "brandPointer": "/brand",
                  "linkPointer": "/url"
                }
                """;
        String json = """
                {"data":{"items":[
                  {"title":"Kettle","brand":"BoilCo","price":{"amount":"1290000"},"url":"https://api.example.com/p/9"},
                  {"title":"","brand":"Skip","price":{"amount":"1"}}
                ]}}
                """;
        when(httpFetcher.get(anyString(), anyString()))
                .thenReturn(new HttpFetcher.FetchResult(200, json, "x"));

        List<SellerCrawler.CrawlItem> items = adapter.crawl(sellerWithConfig(config));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).name()).isEqualTo("Kettle");
        assertThat(items.get(0).price()).isEqualByComparingTo(new BigDecimal("1290000"));
        assertThat(items.get(0).category().name()).isEqualTo("HOME");
    }

    @Test
    void stopsWhenRobotsDisallows() {
        when(robotsTxtService.isAllowed(anyString(), anyString())).thenReturn(false);
        String config = """
                {"listingUrlTemplate":"https://shop.example.com/list?page={page}","itemSelector":"div.card","nameSelector":".t"}
                """;

        List<SellerCrawler.CrawlItem> items = adapter.crawl(sellerWithConfig(config));

        assertThat(items).isEmpty();
        verify(httpFetcher, never()).get(contains("/list"), any());
    }
}
