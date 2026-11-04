package com.pse.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FuzzyMatcherTest {

    private final FuzzyMatcher matcher = new FuzzyMatcher();

    @Test
    void blankQueryMatchesEverything() {
        assertThat(matcher.matches("", "anything")).isTrue();
        assertThat(matcher.matches(null, "anything")).isTrue();
    }

    @Test
    void substringAndCaseInsensitiveMatch() {
        assertThat(matcher.matches("MAC", "MacBook Air")).isTrue();
        assertThat(matcher.matches("phone", "MacBook Air M3")).isFalse();
    }

    @Test
    void toleratesSmallTyposButNotLargeOnes() {
        assertThat(matcher.matches("macbok", "macbook")).isTrue();
        assertThat(matcher.matches("zzzzzz", "macbook")).isFalse();
    }

    @Test
    void nullCandidateNeverMatchesNonBlankQuery() {
        assertThat(matcher.matches("laptop", null)).isFalse();
    }

    @Test
    void similarityIsOneForIdenticalStrings() {
        assertThat(matcher.similarity("laptop", "laptop")).isEqualTo(1.0);
        assertThat(matcher.similarity("laptop", "laptom")).isBetween(0.5, 1.0);
    }
}
