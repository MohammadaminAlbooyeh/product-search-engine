# Product Search Engine

A Spring Boot service that crawls seller catalogues, aggregates prices, and exposes
full-text product search with ranking, price comparison, price-history and
price-drop alerts.

## Stack

| Concern            | Technology                                   |
|--------------------|----------------------------------------------|
| Runtime            | Java 17, Spring Boot 3.3                      |
| Relational store   | PostgreSQL (H2 in the `dev` profile)         |
| Search index       | Elasticsearch 8                              |
| Cache              | Redis (search results, popularity counters)  |
| Messaging          | Kafka (`price.updated`, `product.viewed`)    |
| Migrations         | Flyway (`src/main/resources/db/migration`)   |
| API docs           | springdoc / Swagger UI                       |
| Mail               | Spring Mail (MailHog locally)                |
| Security           | Spring Security (stateless HTTP Basic)       |
| Crawling           | Java HttpClient + jsoup, robots.txt aware    |
| Integration tests  | Testcontainers (Postgres, ES, Kafka, Redis)  |

## Architecture

```text
  Crawlers (per seller)
        |
        v
  AggregationService --(index)--> Elasticsearch <--- SearchService <--- GET /api/search
        |                                                  ^
        | Kafka: price.updated                             | ranking:
        v                                                  | availability + price + popularity
  PriceUpdateConsumer -> PriceAlertService -> e-mail       |
                                                           |
  GET /api/products/{id}                                   |
        |                                                  |
        | Kafka: product.viewed                            |
        v                                                  |
  ProductViewEventConsumer -> ProductPopularityService (Redis) ---------+
```

## Running locally

### 1. Infrastructure

```bash
docker compose -f docker/docker-compose.yml up -d postgres redis elasticsearch kafka mailhog
```

- Postgres: `localhost:5432` (`platform` / `platform`, db `search_db`)
- Elasticsearch: `localhost:9200`
- Redis: `localhost:6379`
- Kafka: `localhost:29092`
- MailHog UI: http://localhost:8025

### 2. Application

```bash
mvn spring-boot:run
```

Runs on port **8086**. Swagger UI: http://localhost:8086/swagger-ui.html
Actuator health: http://localhost:8086/actuator/health

### Dev profile (no external infra)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Uses in-memory H2, disables Flyway and outbound e-mail. Kafka/Redis/Elasticsearch
calls degrade gracefully (logged and skipped) when the brokers are absent.

### Everything in Docker

```bash
docker compose -f docker/docker-compose.yml --profile app up -d --build
```

## Configuration

All settings are environment-overridable (see `application.yml`):

| Variable                     | Default                        | Purpose                          |
|------------------------------|--------------------------------|----------------------------------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | local Postgres      | JDBC connection                  |
| `ELASTICSEARCH_HOST` / `_PORT` | `localhost` / `9200`         | search index                     |
| `REDIS_HOST` / `REDIS_PORT`  | `localhost` / `6379`           | cache + popularity counters      |
| `KAFKA_BOOTSTRAP_SERVERS`    | `localhost:29092`              | messaging                        |
| `MAIL_HOST` / `MAIL_PORT`    | `localhost` / `1025`           | SMTP for price alerts            |
| `NOTIFICATIONS_EMAIL_ENABLED`| `true`                         | set `false` to log instead of send |
| `NOTIFICATIONS_EMAIL_FROM`   | `alerts@product-search-engine.local` | alert sender             |
| `RANKING_POPULARITY_WEIGHT`  | `2.0`                          | weight of view-count in ranking  |
| `CRAWLER_INTERVAL_MS`        | `3600000`                      | crawl scheduler period           |
| `CRAWLER_USER_AGENT`        | `product-search-engine-bot/1.0` | User-Agent sent by the crawler   |
| `CRAWLER_REQUEST_DELAY_MS`  | `1000`                          | min delay between requests per host |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / `admin`     | HTTP Basic credentials for writes |
| `RATE_LIMIT_ENABLED`        | `true`                          | toggle the API rate limiter      |
| `RATE_LIMIT_RPM`            | `120`                           | requests per minute per client   |

## Security

Stateless HTTP Basic. Public, no credentials: `GET /api/search`, `GET /api/products/**`,
`GET /api/sellers/**`, Swagger UI, `/actuator/health`. Everything else (all writes,
other actuator endpoints) needs the `ADMIN` user from `security.admin.*`.

```bash
curl -u admin:admin -XPOST localhost:8086/api/products \
  -H 'Content-Type: application/json' -d '{"id":"p1","name":"Laptop","category":"ELECTRONICS"}'
```

Rate limiting: fixed window, `RATE_LIMIT_RPM` requests/minute per client (keyed by
`X-API-Key` header, else client IP), applied to `/api/**`. Over quota returns
`429` with `Retry-After`. In-memory only - back it with Redis for multi-instance.

## Crawling

Sellers with no `crawlConfig` use the fixture adapters (`DigikalaCrawlerAdapter`,
`GenericSellerCrawlerAdapter`). A seller with a `crawlConfig` JSON is scraped for
real by `HttpSellerCrawlerAdapter`: Java `HttpClient` fetch, jsoup CSS selectors
(or JSON pointers), pagination via a `{page}` token, robots.txt enforcement and
per-host rate limiting.

```bash
curl -u admin:admin -XPOST localhost:8086/api/sellers \
  -H 'Content-Type: application/json' -d '{
    "name": "techshop",
    "url": "https://shop.example.com",
    "crawlConfig": {
      "mode": "html",
      "listingUrlTemplate": "https://shop.example.com/laptops?page={page}",
      "startPage": 1, "maxPages": 5,
      "defaultCategory": "ELECTRONICS",
      "itemSelector": "div.product-card",
      "nameSelector": ".product-title",
      "priceSelector": ".price",
      "brandSelector": ".brand",
      "linkSelector": "a.product-link",
      "imageSelector": "img.product-image"
    }
  }'
```

`mode: "json"` instead uses `itemsPointer` / `namePointer` / `pricePointer` / ...
(RFC 6901 JSON Pointers). Full field list: `SellerCrawlConfig`.

## API

| Method & path                         | Auth  | Description                       |
|---------------------------------------|-------|----------------------------------|
| `GET /api/search`                     | none  | Full-text search + ranking       |
| `GET /api/products/{id}`              | none  | Detail + price history (emits `product.viewed`) |
| `GET /api/products/{id}/compare`      | none  | Price comparison across sellers  |
| `POST /api/products`                  | admin | Register a product               |
| `POST /api/products/{id}/price`       | admin | Upsert a seller price            |
| `DELETE /api/products/{id}`           | admin | Delete a product and its prices  |
| `GET /api/sellers`                    | none  | List crawler targets             |
| `POST` / `DELETE /api/sellers`        | admin | Manage crawler targets           |
| `POST /api/price-alerts`              | admin | Create a price-drop alert        |
| `GET /api/price-alerts/{email}`       | admin | List alerts for an e-mail        |
| `DELETE /api/price-alerts/{id}`       | admin | Deactivate an alert              |

## Tests

```bash
mvn test      # fast unit tests only
mvn verify    # + Testcontainers integration tests (auto-skipped without Docker)
```

Integration tests (`src/test/java/com/pse/integration`) boot the full application
against real Postgres, Elasticsearch, Kafka and Redis containers.

## Known limitations / next steps

- Crawler selectors are configured per seller by hand; there is no auto-discovery
  and no JS rendering (static HTML / JSON endpoints only).
- Rate limiter and popularity counters are in-memory unless Redis-backed.
- Elasticsearch uses the default analyzer (no language-specific analysis).
- No refresh-token / OAuth2 flow - HTTP Basic with a single admin user only.
