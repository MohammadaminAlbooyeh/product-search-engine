package com.pse.crawler.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal robots.txt fetch/parse/cache. Implements the widely supported subset:
 * {@code User-agent}, {@code Disallow} and {@code Allow} with longest-match
 * precedence (Allow wins ties). Unknown directives and wildcards other than a
 * trailing implicit prefix are ignored. A missing or unreachable robots.txt is
 * treated as "everything allowed".
 */
@Component
@Slf4j
public class RobotsTxtService {

    private final HttpFetcher httpFetcher;
    private final long cacheTtlMs;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public RobotsTxtService(HttpFetcher httpFetcher,
                            @Value("${crawler.robots.cache-ttl-ms:3600000}") long cacheTtlMs) {
        this.httpFetcher = httpFetcher;
        this.cacheTtlMs = cacheTtlMs;
    }

    public boolean isAllowed(String url, String userAgent) {
        try {
            URI uri = URI.create(url);
            String origin = uri.getScheme() + "://" + uri.getAuthority();
            String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
            if (uri.getRawQuery() != null) {
                path = path + "?" + uri.getRawQuery();
            }
            Rules rules = rulesFor(origin);
            return rules.allows(path, userAgent == null ? "*" : userAgent);
        } catch (Exception e) {
            log.warn("robots.txt check failed for {} ({}), allowing", url, e.toString());
            return true;
        }
    }

    private Rules rulesFor(String origin) {
        CacheEntry entry = cache.get(origin);
        long now = System.currentTimeMillis();
        if (entry != null && now - entry.fetchedAt < cacheTtlMs) {
            return entry.rules;
        }
        HttpFetcher.FetchResult result = httpFetcher.get(origin + "/robots.txt", null);
        Rules rules = result.isSuccess() ? parse(result.body()) : Rules.allowAll();
        cache.put(origin, new CacheEntry(rules, now));
        return rules;
    }

    static Rules parse(String body) {
        Rules rules = new Rules();
        List<String> currentAgents = new ArrayList<>();
        boolean expectingAgents = false;
        for (String rawLine : body.split("\n")) {
            String line = rawLine;
            int hash = line.indexOf('#');
            if (hash >= 0) {
                line = line.substring(0, hash);
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String field = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            switch (field) {
                case "user-agent" -> {
                    if (!expectingAgents) {
                        currentAgents = new ArrayList<>();
                    }
                    currentAgents.add(value.toLowerCase(Locale.ROOT));
                    expectingAgents = true;
                }
                case "disallow" -> {
                    expectingAgents = false;
                    for (String agent : currentAgents) {
                        rules.add(agent, value, false);
                    }
                }
                case "allow" -> {
                    expectingAgents = false;
                    for (String agent : currentAgents) {
                        rules.add(agent, value, true);
                    }
                }
                default -> expectingAgents = false;
            }
        }
        return rules;
    }

    private record CacheEntry(Rules rules, long fetchedAt) {
    }

    static final class Rules {
        private final Map<String, List<Rule>> byAgent = new ConcurrentHashMap<>();

        static Rules allowAll() {
            return new Rules();
        }

        void add(String agent, String path, boolean allow) {
            byAgent.computeIfAbsent(agent, k -> new ArrayList<>()).add(new Rule(path, allow));
        }

        boolean allows(String path, String userAgent) {
            List<Rule> rules = resolve(userAgent);
            if (rules == null || rules.isEmpty()) {
                return true;
            }
            Rule best = null;
            for (Rule rule : rules) {
                if (rule.matches(path) && (best == null || rule.length() > best.length()
                        || (rule.length() == best.length() && rule.allow()))) {
                    best = rule;
                }
            }
            return best == null || best.allow();
        }

        private List<Rule> resolve(String userAgent) {
            String ua = userAgent.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, List<Rule>> e : byAgent.entrySet()) {
                String token = e.getKey();
                if (!token.equals("*") && !token.isEmpty() && ua.contains(token)) {
                    return e.getValue();
                }
            }
            return byAgent.get("*");
        }
    }

    private record Rule(String path, boolean allow) {
        boolean matches(String requestPath) {
            if (path.isEmpty()) {
                return false;
            }
            return requestPath.startsWith(path);
        }

        int length() {
            return path.length();
        }
    }
}
