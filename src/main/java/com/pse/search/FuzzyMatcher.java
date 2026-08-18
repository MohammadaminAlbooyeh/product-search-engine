package com.pse.search;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class FuzzyMatcher {

    public boolean matches(String query, String candidate) {
        if (query == null || query.isBlank()) {
            return true;
        }
        if (candidate == null) {
            return false;
        }
        String q = normalize(query);
        String c = normalize(candidate);
        if (c.contains(q)) {
            return true;
        }
        return levenshtein(q, c) <= Math.max(1, q.length() / 3);
    }

    public double similarity(String query, String candidate) {
        if (query == null || candidate == null) {
            return 0;
        }
        String q = normalize(query);
        String c = normalize(candidate);
        if (q.isEmpty()) {
            return 0;
        }
        int distance = levenshtein(q, c);
        return 1.0 - ((double) distance / Math.max(q.length(), c.length()));
    }

    private String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[\\s،,]+", " ").trim();
    }

    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}
