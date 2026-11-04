package com.pse.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PriceNormalizerTest {

    private final PriceNormalizer normalizer = new PriceNormalizer();

    @Test
    void stripsCurrencyAndGroupingNoise() {
        assertThat(normalizer.normalize("$45,900,000")).isEqualByComparingTo(new BigDecimal("45900000"));
        assertThat(normalizer.normalize(" 1 299 000 TL ")).isEqualByComparingTo(new BigDecimal("1299000"));
    }

    @Test
    void handlesMultipleDotsAsThousandsSeparators() {
        assertThat(normalizer.normalize("1.299.000")).isEqualByComparingTo(new BigDecimal("1299000"));
    }

    @Test
    void keepsSingleDecimalPoint() {
        assertThat(normalizer.normalize("12.50")).isEqualByComparingTo(new BigDecimal("12.50"));
    }

    @Test
    void returnsNullForMissingOrNonNumericInput() {
        assertThat(normalizer.normalize(null)).isNull();
        assertThat(normalizer.normalize("out of stock")).isNull();
        assertThat(normalizer.normalize(".")).isNull();
    }
}
