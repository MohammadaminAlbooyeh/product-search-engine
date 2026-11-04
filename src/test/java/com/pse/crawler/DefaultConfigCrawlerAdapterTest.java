package com.pse.crawler;

import com.pse.crawler.config.SellerCrawlConfig;
import com.pse.model.entity.Seller;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DefaultConfigCrawlerAdapterTest {

    private final HttpSellerCrawlerAdapter delegate = mock(HttpSellerCrawlerAdapter.class);

    @Test
    void amazonAdapterSupportsAmazonDomainsOnly() {
        AmazonCrawlerAdapter adapter = new AmazonCrawlerAdapter(delegate);

        assertThat(adapter.supports(Seller.builder().name("amz").url("https://www.amazon.de").build())).isTrue();
        assertThat(adapter.supports(Seller.builder().name("x").url("https://www.ebay.com").build())).isFalse();
        assertThat(adapter.supports(Seller.builder().name("x").url("https://www.amazon.com")
                .crawlConfig("{}").build())).isFalse();
    }

    @Test
    void ebayAdapterSupportsEbayDomainsOnly() {
        EbayCrawlerAdapter adapter = new EbayCrawlerAdapter(delegate);

        assertThat(adapter.supports(Seller.builder().name("ebay").url("https://www.ebay.co.uk").build())).isTrue();
        assertThat(adapter.supports(Seller.builder().name("x").url("https://www.amazon.com").build())).isFalse();
    }

    @Test
    void amazonDelegatesWithBuiltInSelectors() {
        AmazonCrawlerAdapter adapter = new AmazonCrawlerAdapter(delegate);
        Seller seller = Seller.builder().name("amz").url("https://www.amazon.com").build();
        when(delegate.crawlWithConfig(eq(seller), any())).thenReturn(List.of());

        adapter.crawl(seller);

        ArgumentCaptor<SellerCrawlConfig> captor = ArgumentCaptor.forClass(SellerCrawlConfig.class);
        verify(delegate).crawlWithConfig(eq(seller), captor.capture());
        SellerCrawlConfig config = captor.getValue();
        assertThat(config.mode()).isEqualTo("html");
        assertThat(config.listingUrlTemplate()).contains("/s?k=").contains("{page}");
        assertThat(config.itemSelector()).contains("s-search-result");
        assertThat(config.nameSelector()).isEqualTo("h2 a span");
    }

    @Test
    void ebayDelegatesWithBuiltInSelectors() {
        EbayCrawlerAdapter adapter = new EbayCrawlerAdapter(delegate);
        Seller seller = Seller.builder().name("ebay").url("https://www.ebay.com/").build();
        when(delegate.crawlWithConfig(eq(seller), any())).thenReturn(List.of());

        adapter.crawl(seller);

        ArgumentCaptor<SellerCrawlConfig> captor = ArgumentCaptor.forClass(SellerCrawlConfig.class);
        verify(delegate).crawlWithConfig(eq(seller), captor.capture());
        SellerCrawlConfig config = captor.getValue();
        assertThat(config.listingUrlTemplate()).startsWith("https://www.ebay.com/sch/").doesNotContain("//sch");
        assertThat(config.itemSelector()).isEqualTo("li.s-item");
        assertThat(config.linkSelector()).isEqualTo("a.s-item__link");
    }
}
