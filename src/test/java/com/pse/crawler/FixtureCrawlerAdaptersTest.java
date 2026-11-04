package com.pse.crawler;

import com.pse.model.entity.Seller;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FixtureCrawlerAdaptersTest {

    private final DigikalaCrawlerAdapter digikala = new DigikalaCrawlerAdapter();
    private final GenericSellerCrawlerAdapter generic = new GenericSellerCrawlerAdapter();

    @Test
    void digikalaSupportsOnlyDigikalaSellers() {
        assertThat(digikala.supports(Seller.builder().name("digikala").digikala(true).build())).isTrue();
        assertThat(digikala.supports(Seller.builder().name("techshop").digikala(false).build())).isFalse();
    }

    @Test
    void genericSupportsNonDigikalaSellersWithoutCrawlConfig() {
        assertThat(generic.supports(Seller.builder().name("techshop").build())).isTrue();
        assertThat(generic.supports(Seller.builder().name("digikala").digikala(true).build())).isFalse();
        assertThat(generic.supports(Seller.builder().name("configured").crawlConfig("{}").build())).isFalse();
    }

    @Test
    void digikalaReturnsCatalogueWithAvailabilityVariants() {
        List<SellerCrawler.CrawlItem> items = digikala.crawl(
                Seller.builder().name("digikala").url("https://digikala.com").digikala(true).build());

        assertThat(items).hasSizeGreaterThanOrEqualTo(3);
        assertThat(items).allSatisfy(item -> {
            assertThat(item.name()).isNotBlank();
            assertThat(item.price()).isPositive();
            assertThat(item.url()).isEqualTo("https://digikala.com");
        });
        assertThat(items).extracting(SellerCrawler.CrawlItem::availability)
                .contains(com.pse.model.enums.AvailabilityStatus.IN_STOCK,
                        com.pse.model.enums.AvailabilityStatus.LIMITED,
                        com.pse.model.enums.AvailabilityStatus.OUT_OF_STOCK);
    }

    @Test
    void genericReturnsDistinctCategoriesAndPrices() {
        List<SellerCrawler.CrawlItem> items = generic.crawl(
                Seller.builder().name("techshop").url("https://techshop.example.com").build());

        assertThat(items).hasSize(3);
        assertThat(items).allSatisfy(item -> assertThat(item.price()).isPositive());
        assertThat(items).extracting(SellerCrawler.CrawlItem::name).doesNotHaveDuplicates();
    }
}
