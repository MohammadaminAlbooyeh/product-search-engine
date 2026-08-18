package com.pse.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PriceNormalizer {

    private static final String PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹";
    private static final String ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩";
    private static final String ASCII_DIGITS = "0123456789";

    public BigDecimal normalize(String rawPrice) {
        if (rawPrice == null) {
            return null;
        }
        String cleaned = rawPrice
                .replace("تومان", "")
                .replace("ریال", "")
                .replace("٬", "")
                .replace(",", "")
                .replace("،", "")
                .trim();

        StringBuilder ascii = new StringBuilder();
        for (char c : cleaned.toCharArray()) {
            int pIdx = PERSIAN_DIGITS.indexOf(c);
            if (pIdx >= 0) {
                ascii.append(ASCII_DIGITS.charAt(pIdx));
                continue;
            }
            int aIdx = ARABIC_DIGITS.indexOf(c);
            if (aIdx >= 0) {
                ascii.append(ASCII_DIGITS.charAt(aIdx));
                continue;
            }
            ascii.append(c);
        }
        try {
            return new BigDecimal(ascii.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
