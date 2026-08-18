package com.pse.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductIdGenerator {

    public String generate() {
        return "p-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
