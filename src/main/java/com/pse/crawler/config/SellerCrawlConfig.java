package com.pse.crawler.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pse.model.enums.ProductCategory;

/**
 * Per-seller crawl configuration, stored as JSON on {@code sellers.crawl_config}.
 *
 * <p>Two modes are supported:
 * <ul>
 *   <li>{@code html} - fetch {@link #listingUrlTemplate} and extract product cards
 *       with CSS selectors ({@link #itemSelector} and friends, jsoup syntax).</li>
 *   <li>{@code json} - fetch {@link #listingUrlTemplate} and read an array at
 *       {@link #itemsPointer} (JSON Pointer, RFC 6901), then read each field with a
 *       pointer relative to the array element.</li>
 * </ul>
 *
 * <p>{@link #listingUrlTemplate} may contain the literal token {@code {page}} which
 * is substituted with the current page number for pagination.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SellerCrawlConfig(
        String mode,
        String userAgent,
        String listingUrlTemplate,
        Integer startPage,
        Integer maxPages,
        Long requestDelayMs,
        Boolean respectRobots,
        ProductCategory defaultCategory,

        // ---- html mode ----
        String itemSelector,
        String nameSelector,
        String priceSelector,
        String brandSelector,
        String linkSelector,
        String linkAttribute,
        String imageSelector,
        String imageAttribute,
        String availabilitySelector,
        String inStockText,

        // ---- json mode ----
        String itemsPointer,
        String namePointer,
        String pricePointer,
        String brandPointer,
        String linkPointer,
        String imagePointer,
        String availabilityPointer) {

    public boolean isJsonMode() {
        return "json".equalsIgnoreCase(mode);
    }

    public int startPageOrDefault() {
        return startPage != null && startPage > 0 ? startPage : 1;
    }

    public int maxPagesOrDefault() {
        return maxPages != null && maxPages > 0 ? maxPages : 1;
    }

    public boolean respectRobotsOrDefault() {
        return respectRobots == null || respectRobots;
    }

    public String linkAttributeOrDefault() {
        return linkAttribute != null && !linkAttribute.isBlank() ? linkAttribute : "href";
    }

    public String imageAttributeOrDefault() {
        return imageAttribute != null && !imageAttribute.isBlank() ? imageAttribute : "src";
    }

    public ProductCategory defaultCategoryOrOther() {
        return defaultCategory != null ? defaultCategory : ProductCategory.OTHER;
    }
}
