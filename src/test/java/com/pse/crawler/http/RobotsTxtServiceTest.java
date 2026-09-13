package com.pse.crawler.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RobotsTxtServiceTest {

    @Test
    void disallowBlocksMatchingPrefix() {
        RobotsTxtService.Rules rules = RobotsTxtService.parse("""
                User-agent: *
                Disallow: /private
                Disallow: /cart
                """);

        assertThat(rules.allows("/private/orders", "somebot")).isFalse();
        assertThat(rules.allows("/cart", "somebot")).isFalse();
        assertThat(rules.allows("/products?page=1", "somebot")).isTrue();
    }

    @Test
    void allowOverridesLongerDisallowByLength() {
        RobotsTxtService.Rules rules = RobotsTxtService.parse("""
                User-agent: *
                Disallow: /products
                Allow: /products/public
                """);

        assertThat(rules.allows("/products/secret", "bot")).isFalse();
        assertThat(rules.allows("/products/public/item-1", "bot")).isTrue();
    }

    @Test
    void agentSpecificGroupWins() {
        RobotsTxtService.Rules rules = RobotsTxtService.parse("""
                User-agent: *
                Disallow: /

                User-agent: product-search-engine-bot
                Disallow: /admin
                """);

        assertThat(rules.allows("/products", "product-search-engine-bot/1.0")).isTrue();
        assertThat(rules.allows("/admin", "product-search-engine-bot/1.0")).isFalse();
        assertThat(rules.allows("/products", "randomcrawler")).isFalse();
    }

    @Test
    void emptyRobotsAllowsEverything() {
        assertThat(RobotsTxtService.parse("").allows("/anything", "bot")).isTrue();
    }
}
