package com.pse.crawler;

import com.pse.model.entity.Seller;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlerFactoryTest {

    private final CrawlerFactory factory = new CrawlerFactory(List.of(
            new GenericSellerCrawlerAdapter(),
            new DigikalaCrawlerAdapter()));

    @Test
    void digikalaSellerUsesDigikalaAdapter() {
        Seller seller = Seller.builder().name("digikala").digikala(true).build();
        assertThat(factory.forSeller(seller)).isInstanceOf(DigikalaCrawlerAdapter.class);
    }

    @Test
    void genericSellerUsesGenericAdapter() {
        Seller seller = Seller.builder().name("techshop").digikala(false).build();
        assertThat(factory.forSeller(seller)).isInstanceOf(GenericSellerCrawlerAdapter.class);
    }

    @Test
    void digikalaAdapterReturnsProducts() {
        Seller seller = Seller.builder().name("digikala").url("https://digikala.com").digikala(true).build();
        List<SellerCrawler.CrawlItem> items = new DigikalaCrawlerAdapter().crawl(seller);
        assertThat(items).isNotEmpty();
        assertThat(items.get(0).price()).isPositive();
    }
}