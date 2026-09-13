package com.pse.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pse.crawler.config.SellerCrawlConfig;
import com.pse.crawler.http.CrawlRateLimiter;
import com.pse.crawler.http.HttpFetcher;
import com.pse.crawler.http.RobotsTxtService;
import com.pse.exception.CrawlerFailedException;
import com.pse.model.entity.Seller;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.util.PriceNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Real crawler adapter: fetches a seller's listing pages over HTTP and extracts
 * products via CSS selectors (HTML mode) or JSON pointers (JSON mode). Handles
 * pagination, robots.txt and per-host rate limiting.
 *
 * <p>Activated for any {@link Seller} that carries a non-blank
 * {@code crawlConfig}. Sellers without a config fall through to the fixture
 * adapters.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class HttpSellerCrawlerAdapter implements SellerCrawler {

    private final HttpFetcher httpFetcher;
    private final RobotsTxtService robotsTxtService;
    private final CrawlRateLimiter rateLimiter;
    private final PriceNormalizer priceNormalizer;
    private final ObjectMapper objectMapper;
    private final long defaultDelayMs;

    public HttpSellerCrawlerAdapter(HttpFetcher httpFetcher,
                                    RobotsTxtService robotsTxtService,
                                    CrawlRateLimiter rateLimiter,
                                    PriceNormalizer priceNormalizer,
                                    ObjectMapper objectMapper,
                                    @Value("${crawler.http.request-delay-ms:1000}") long defaultDelayMs) {
        this.httpFetcher = httpFetcher;
        this.robotsTxtService = robotsTxtService;
        this.rateLimiter = rateLimiter;
        this.priceNormalizer = priceNormalizer;
        this.objectMapper = objectMapper;
        this.defaultDelayMs = defaultDelayMs;
    }

    @Override
    public boolean supports(Seller seller) {
        return seller.getCrawlConfig() != null && !seller.getCrawlConfig().isBlank();
    }

    @Override
    public List<CrawlItem> crawl(Seller seller) throws CrawlerFailedException {
        SellerCrawlConfig config = parseConfig(seller);
        if (config.listingUrlTemplate() == null || config.listingUrlTemplate().isBlank()) {
            throw new CrawlerFailedException("crawlConfig.listingUrlTemplate is required for seller " + seller.getName());
        }
        String userAgent = config.userAgent() != null ? config.userAgent() : httpFetcher.defaultUserAgent();
        long delayMs = config.requestDelayMs() != null ? config.requestDelayMs() : defaultDelayMs;

        List<CrawlItem> items = new ArrayList<>();
        int firstPage = config.startPageOrDefault();
        int lastPage = firstPage + config.maxPagesOrDefault() - 1;

        for (int page = firstPage; page <= lastPage; page++) {
            String url = config.listingUrlTemplate().replace("{page}", Integer.toString(page));

            if (config.respectRobotsOrDefault() && !robotsTxtService.isAllowed(url, userAgent)) {
                log.info("robots.txt disallows {} for {}, stopping", url, seller.getName());
                break;
            }
            rateLimiter.acquire(hostOf(url), delayMs);

            HttpFetcher.FetchResult result = httpFetcher.get(url, userAgent);
            if (!result.isSuccess()) {
                log.warn("Seller {} page {} returned status {}", seller.getName(), page, result.status());
                break;
            }

            List<CrawlItem> pageItems = config.isJsonMode()
                    ? parseJson(result.body(), config, url)
                    : parseHtml(result.body(), config, result.finalUrl());

            if (pageItems.isEmpty()) {
                log.debug("Seller {} page {} yielded no items, stopping pagination", seller.getName(), page);
                break;
            }
            items.addAll(pageItems);
        }
        log.info("HTTP crawl of {} produced {} items", seller.getName(), items.size());
        return items;
    }

    private SellerCrawlConfig parseConfig(Seller seller) {
        try {
            return objectMapper.readValue(seller.getCrawlConfig(), SellerCrawlConfig.class);
        } catch (Exception e) {
            throw new CrawlerFailedException("Invalid crawlConfig JSON for seller " + seller.getName(), e);
        }
    }

    // ---------------------------------------------------------------- HTML ----

    private List<CrawlItem> parseHtml(String body, SellerCrawlConfig cfg, String baseUri) {
        List<CrawlItem> out = new ArrayList<>();
        Document doc = Jsoup.parse(body, baseUri == null ? "" : baseUri);
        if (cfg.itemSelector() == null || cfg.itemSelector().isBlank()) {
            log.warn("html mode requires itemSelector");
            return out;
        }
        for (Element card : doc.select(cfg.itemSelector())) {
            String name = text(card, cfg.nameSelector());
            if (name == null || name.isBlank()) {
                continue;
            }
            BigDecimal price = priceNormalizer.normalize(text(card, cfg.priceSelector()));
            String brand = text(card, cfg.brandSelector());
            String link = attr(card, cfg.linkSelector(), cfg.linkAttributeOrDefault());
            String image = attr(card, cfg.imageSelector(), cfg.imageAttributeOrDefault());
            String availabilityText = text(card, cfg.availabilitySelector());
            out.add(new CrawlItem(null, name.trim(), blankToNull(brand),
                    cfg.defaultCategoryOrOther(), blankToNull(image), price,
                    blankToNull(link), availability(price, availabilityText, cfg.inStockText())));
        }
        return out;
    }

    private String text(Element scope, String selector) {
        if (selector == null || selector.isBlank()) {
            return null;
        }
        Element el = scope.selectFirst(selector);
        return el == null ? null : el.text();
    }

    private String attr(Element scope, String selector, String attribute) {
        if (selector == null || selector.isBlank()) {
            return null;
        }
        Element el = scope.selectFirst(selector);
        return el == null ? null : el.absUrl(attribute).isBlank() ? el.attr(attribute) : el.absUrl(attribute);
    }

    // ---------------------------------------------------------------- JSON ----

    private List<CrawlItem> parseJson(String body, SellerCrawlConfig cfg, String url) {
        List<CrawlItem> out = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = cfg.itemsPointer() == null || cfg.itemsPointer().isBlank()
                    ? root : root.at(cfg.itemsPointer());
            if (!items.isArray()) {
                log.warn("json mode: itemsPointer {} did not resolve to an array", cfg.itemsPointer());
                return out;
            }
            for (JsonNode node : items) {
                String name = pointer(node, cfg.namePointer());
                if (name == null || name.isBlank()) {
                    continue;
                }
                BigDecimal price = priceNormalizer.normalize(pointer(node, cfg.pricePointer()));
                String link = pointer(node, cfg.linkPointer());
                out.add(new CrawlItem(null, name.trim(), blankToNull(pointer(node, cfg.brandPointer())),
                        cfg.defaultCategoryOrOther(), blankToNull(pointer(node, cfg.imagePointer())), price,
                        blankToNull(link),
                        availability(price, pointer(node, cfg.availabilityPointer()), cfg.inStockText())));
            }
        } catch (Exception e) {
            log.warn("json parse failed for {}: {}", url, e.toString());
        }
        return out;
    }

    private String pointer(JsonNode node, String pointer) {
        if (pointer == null || pointer.isBlank()) {
            return null;
        }
        JsonNode value = node.at(pointer);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    // -------------------------------------------------------------- helpers ----

    private AvailabilityStatus availability(BigDecimal price, String availabilityText, String inStockText) {
        if (availabilityText != null && !availabilityText.isBlank()) {
            String needle = inStockText != null && !inStockText.isBlank() ? inStockText : "in stock";
            return availabilityText.toLowerCase().contains(needle.toLowerCase())
                    ? AvailabilityStatus.IN_STOCK : AvailabilityStatus.OUT_OF_STOCK;
        }
        return price != null ? AvailabilityStatus.IN_STOCK : AvailabilityStatus.OUT_OF_STOCK;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }
}
