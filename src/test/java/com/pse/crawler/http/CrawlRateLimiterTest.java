package com.pse.crawler.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlRateLimiterTest {

    private final CrawlRateLimiter limiter = new CrawlRateLimiter();

    @Test
    void secondCallToSameHostIsDelayed() {
        long start = System.currentTimeMillis();
        limiter.acquire("example.com", 200);
        limiter.acquire("example.com", 200);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(180);
    }

    @Test
    void differentHostsAreIndependent() {
        long start = System.currentTimeMillis();
        limiter.acquire("a.example.com", 500);
        limiter.acquire("b.example.com", 500);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(200);
    }

    @Test
    void zeroDelayNeverBlocks() {
        long start = System.currentTimeMillis();
        limiter.acquire("example.com", 0);
        limiter.acquire("example.com", 0);
        assertThat(System.currentTimeMillis() - start).isLessThan(50);
    }
}
