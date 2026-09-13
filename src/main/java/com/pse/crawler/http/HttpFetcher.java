package com.pse.crawler.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin wrapper around {@link HttpClient} for crawler fetches: sane timeouts, a
 * configurable User-Agent and redirect following. Never throws for HTTP-level
 * errors - callers inspect {@link FetchResult#status()}.
 */
@Component
@Slf4j
public class HttpFetcher {

    private final HttpClient client;
    private final String defaultUserAgent;
    private final Duration requestTimeout;

    public HttpFetcher(
            @Value("${crawler.http.user-agent:product-search-engine-bot/1.0 (+https://example.com/bot)}") String defaultUserAgent,
            @Value("${crawler.http.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${crawler.http.request-timeout-ms:10000}") long requestTimeoutMs) {
        this.defaultUserAgent = defaultUserAgent;
        this.requestTimeout = Duration.ofMillis(requestTimeoutMs);
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String defaultUserAgent() {
        return defaultUserAgent;
    }

    public FetchResult get(String url, String userAgent) {
        String ua = userAgent != null && !userAgent.isBlank() ? userAgent : defaultUserAgent;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(requestTimeout)
                    .header("User-Agent", ua)
                    .header("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new FetchResult(response.statusCode(), response.body(), response.uri().toString());
        } catch (Exception e) {
            log.warn("Fetch failed for {}: {}", url, e.toString());
            return new FetchResult(0, null, url);
        }
    }

    public record FetchResult(int status, String body, String finalUrl) {
        public boolean isSuccess() {
            return status >= 200 && status < 300 && body != null;
        }
    }
}
