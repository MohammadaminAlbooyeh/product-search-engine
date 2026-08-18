package com.pse.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pse.model.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchResultCacheService {

    private static final String PREFIX = "search:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public List<ProductDocument> get(String key) {
        try {
            String json = redisTemplate.opsForValue().get(PREFIX + key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("Redis cache read failed for {}: {}", key, e.getMessage());
            return null;
        }
    }

    public void put(String key, List<ProductDocument> results) {
        try {
            redisTemplate.opsForValue().set(PREFIX + key, objectMapper.writeValueAsString(results), TTL);
        } catch (Exception e) {
            log.warn("Redis cache write failed for {}: {}", key, e.getMessage());
        }
    }

    public void evict(String key) {
        try {
            redisTemplate.delete(PREFIX + key);
        } catch (Exception e) {
            log.warn("Redis cache evict failed for {}: {}", key, e.getMessage());
        }
    }
}
