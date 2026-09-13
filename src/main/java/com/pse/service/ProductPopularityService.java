package com.pse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks how often each product is viewed so that {@link RankingService} can boost
 * popular products. Backed by Redis (shared across instances) with an in-memory
 * fallback when Redis is unavailable.
 */
@Service
@Slf4j
public class ProductPopularityService {

    private static final String KEY = "popularity:views";

    @Nullable
    private final StringRedisTemplate redisTemplate;
    private final Map<String, AtomicLong> fallback = new ConcurrentHashMap<>();

    public ProductPopularityService(@Nullable StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordView(String productId) {
        if (productId == null || productId.isBlank()) {
            return;
        }
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForHash().increment(KEY, productId, 1);
                return;
            } catch (Exception e) {
                log.warn("Redis unavailable, counting view for {} in memory: {}", productId, e.getMessage());
            }
        }
        fallback.computeIfAbsent(productId, k -> new AtomicLong()).incrementAndGet();
    }

    public long viewCount(String productId) {
        if (productId == null) {
            return 0;
        }
        if (redisTemplate != null) {
            try {
                Object value = redisTemplate.opsForHash().get(KEY, productId);
                return value == null ? 0 : Long.parseLong(String.valueOf(value));
            } catch (Exception e) {
                log.warn("Redis unavailable, reading view count for {} from memory: {}", productId, e.getMessage());
            }
        }
        AtomicLong counter = fallback.get(productId);
        return counter == null ? 0 : counter.get();
    }

    /**
     * Logarithmic boost so that a handful of views helps a little and viral products
     * do not completely dominate ranking.
     */
    public double popularityBoost(String productId) {
        return Math.log10(1 + viewCount(productId));
    }
}
