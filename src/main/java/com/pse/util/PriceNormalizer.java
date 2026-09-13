package com.pse.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Normalizes raw price strings scraped from seller pages into a {@link BigDecimal}.
 *
 * <p>Strips grouping separators and any non-numeric noise (currency labels, units,
 * whitespace), then parses what remains. Multiple dots are treated as thousands
 * separators and removed.
 */
@Component
public class PriceNormalizer {

    public BigDecimal normalize(String rawPrice) {
        if (rawPrice == null) {
            return null;
        }

        String digits = rawPrice.replaceAll("[^0-9.]", "");
        long dotCount = digits.chars().filter(ch -> ch == '.').count();
        if (dotCount > 1) {
            // Multiple dots can only be thousands separators here.
            digits = digits.replace(".", "");
        }
        if (digits.isEmpty() || ".".equals(digits)) {
            return null;
        }
        try {
            return new BigDecimal(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
