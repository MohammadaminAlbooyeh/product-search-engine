package com.pse.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPopularityServiceTest {

    private final ProductPopularityService service = new ProductPopularityService(null);

    @Test
    void recordsViewsInMemoryWhenRedisAbsent() {
        service.recordView("p1");
        service.recordView("p1");
        service.recordView("p2");

        assertThat(service.viewCount("p1")).isEqualTo(2);
        assertThat(service.viewCount("p2")).isEqualTo(1);
        assertThat(service.viewCount("unknown")).isZero();
    }

    @Test
    void ignoresBlankProductId() {
        service.recordView("");
        service.recordView(null);
        assertThat(service.viewCount("")).isZero();
    }

    @Test
    void popularityBoostGrowsWithViewsButDampened() {
        ProductPopularityService s = new ProductPopularityService(null);
        for (int i = 0; i < 9; i++) {
            s.recordView("p1");
        }
        double boostAt9 = s.popularityBoost("p1");
        for (int i = 0; i < 90; i++) {
            s.recordView("p1");
        }
        double boostAt99 = s.popularityBoost("p1");

        assertThat(boostAt9).isGreaterThan(0.0);
        assertThat(boostAt99).isGreaterThan(boostAt9);
        // logarithmic: 10x the views is roughly +1, not 10x the boost
        assertThat(boostAt99 - boostAt9).isBetween(0.9, 1.1);
    }
}
