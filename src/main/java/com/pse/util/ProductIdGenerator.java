package com.pse.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class ProductIdGenerator {

    public String generate() {
        return "p-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Stable id derived from a seed (e.g. a product URL or seller + name), so that
     * re-crawling the same catalogue upserts existing products instead of creating
     * duplicates.
     */
    public String deterministic(String seed) {
        if (seed == null || seed.isBlank()) {
            return generate();
        }
        return "p-" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8))
                .toString().substring(0, 12);
    }
}
