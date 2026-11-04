package com.pse.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Central place for the custom business metrics exposed at
 * {@code /actuator/prometheus}.
 */
@Component
@Slf4j
public class ApplicationMetrics {

    private final MeterRegistry registry;

    public ApplicationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordSearch(Duration latency, int results, boolean cacheHit) {
        Timer.builder("pse.search.latency")
                .description("Time spent serving a search request")
                .tag("cache", cacheHit ? "hit" : "miss")
                .register(registry)
                .record(latency.toNanos(), TimeUnit.NANOSECONDS);
        registry.summary("pse.search.results", "cache", cacheHit ? "hit" : "miss")
                .record(results);
    }

    public void recordCrawl(String seller, int items, boolean success, Duration latency) {
        Timer.builder("pse.crawl.duration")
                .description("Time spent crawling one seller")
                .tag("seller", seller)
                .tag("outcome", success ? "success" : "failure")
                .register(registry)
                .record(latency.toNanos(), TimeUnit.NANOSECONDS);
        Counter.builder("pse.crawl.items")
                .description("Products produced by crawls")
                .tag("seller", seller)
                .register(registry)
                .increment(items);
    }

    public void recordAlertFired() {
        Counter.builder("pse.alerts.fired")
                .description("Price-drop alerts triggered")
                .register(registry)
                .increment();
    }
}
