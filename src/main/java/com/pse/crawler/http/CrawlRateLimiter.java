package com.pse.crawler.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces a minimum delay between consecutive requests to the same host so the
 * crawler stays polite. Thread-safe; blocks the calling thread when needed.
 */
@Component
@Slf4j
public class CrawlRateLimiter {

    private final ConcurrentHashMap<String, Long> lastRequestAt = new ConcurrentHashMap<>();

    public void acquire(String host, long minDelayMs) {
        if (host == null || minDelayMs <= 0) {
            return;
        }
        long waitFor;
        synchronized (internedHost(host)) {
            long now = System.currentTimeMillis();
            Long previous = lastRequestAt.get(host);
            if (previous != null) {
                long elapsed = now - previous;
                waitFor = Math.max(0, minDelayMs - elapsed);
            } else {
                waitFor = 0;
            }
            lastRequestAt.put(host, now + waitFor);
        }
        if (waitFor > 0) {
            try {
                Thread.sleep(waitFor);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Stable per-host monitor object. */
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    private Object internedHost(String host) {
        return locks.computeIfAbsent(host, k -> new Object());
    }
}
