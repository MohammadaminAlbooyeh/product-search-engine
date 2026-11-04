# Product Search Engine

A Spring Boot service that crawls seller catalogues, aggregates prices, and exposes
full-text product search with ranking, price comparison, price history and
price-drop alerts. Ships with a browser UI, JWT accounts, Prometheus/Grafana
observability and Kubernetes manifests.

## Stack

| Concern            | Technology                                        |
|--------------------|---------------------------------------------------|
| Runtime            | Java 17, Spring Boot 3.3                           |
| Relational store   | PostgreSQL (H2 in the `dev` profile)              |
| Search index       | Elasticsearch 8                                   |
| Cache              | Redis (search results, popularity counters)       |
| Messaging          | Kafka (`price.updated`, `product.viewed`)         |
| Migrations         | Flyway (`src/main/resources/db/migration`)        |
| API docs           | springdoc / Swagger UI                            |
| Mail               | Spring Mail (MailHog locally)                     |
| Security           | JWT bearer for users + HTTP Basic bootstrap admin |
| Crawling           | Java HttpClient + jsoup, robots.txt aware         |
| Observability      | Actuator, Micrometer/Prometheus, Brave tracing, JSON logs |
| UI                 | Static HTML/CSS/JS served by the app              |
| Integration tests  | Testcontainers (Postgres, ES, Kafka, Redis)       |

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
        v                                                  v
  ProductViewEventConsumer -> ProductPopularityService (Redis) ---------+

  Browser UI (static) -> /api/search, /api/products, /api/price-alerts, /api/auth
```

## Running locally

### 1. Infrastructure

```bash
docker compose -f docker/docker-compose.yml up -d \
  postgres redis elasticsearch kafka mailhog zipkin prometheus grafana
```

- Postgres: `localhost:5432` (`platform` / `platform`, db `search_db`)
- Elasticsearch: `localhost:9200`
- Redis: `localhost:6379`
- Kafka: `localhost:29092`
- MailHog UI: http://localhost:8025
- Zipkin: http://localhost:9411
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (`admin` / `admin` by default)

### 2. Application

```bash
mvn spring-boot:run
```

- UI: http://localhost:8086/
- Swagger UI: http://localhost:8086/swagger-ui.html
- Health: http://localhost:8086/actuator/health
- Prometheus metrics: http://localhost:8086/actuator/prometheus

### Dev profile (no external infra)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Uses in-memory H2, disables Flyway and outbound e-mail. Kafka/Redis/Elasticsearch
calls degrade gracefully (logged and skipped) when the brokers are absent.

### Everything in Docker

```bash
cp .env.example .env          # change the passwords and JWT secret
docker compose -f docker/docker-compose.yml --profile app up -d --build
```

The `app` service runs with the `prod` profile: JSON logs, no default credentials
and Prometheus metrics enabled.

## Authentication

Two mechanisms coexist:

- **JWT bearer** for regular users (register/login), used by the UI and price alerts.
- **HTTP Basic** for the bootstrap `ADMIN` account (product/seller writes).

```bash
# Register (returns a JWT)
curl -XPOST localhost:8086/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"buyer@example.com","password":"password123","displayName":"Buyer"}'

# Login
curl -XPOST localhost:8086/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"buyer@example.com","password":"password123"}'

# Use the token
curl localhost:8086/api/price-alerts/me -H "Authorization: Bearer $TOKEN"

# Bootstrap admin (writes)
curl -u admin:admin -XPOST localhost:8086/api/products \
  -H 'Content-Type: application/json' \
  -d '{"id":"p1","name":"Laptop","category":"ELECTRONICS"}'
```

RBAC: public reads (search, products, sellers), self-or-admin price-alert reads,
`ROLE_ADMIN` for product/seller writes. Alert ownership is derived from the JWT,
never trusted from the request body.

## Configuration

All settings are environment-overridable (see `application.yml`):

| Variable                     | Default                        | Purpose                          |
|------------------------------|--------------------------------|----------------------------------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | local Postgres      | JDBC connection                  |
| `ELASTICSEARCH_HOST` / `_PORT` | `localhost` / `9200`         | search index                     |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `localhost` / `6379` | cache + popularity counters |
| `KAFKA_BOOTSTRAP_SERVERS`    | `localhost:29092`              | messaging                        |
| `MAIL_HOST` / `MAIL_PORT`    | `localhost` / `1025`           | SMTP for price alerts            |
| `NOTIFICATIONS_EMAIL_ENABLED`| `true`                         | set `false` to log instead of send |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / `admin`       | HTTP Basic credentials for writes |
| `SECURITY_JWT_SECRET`        | dev-only value                 | HS256 secret, min 32 bytes       |
| `SECURITY_JWT_EXPIRATION_SECONDS` | `3600`                    | token lifetime                   |
| `RATE_LIMIT_ENABLED`         | `true`                         | toggle the API rate limiter      |
| `RATE_LIMIT_RPM`             | `120`                          | requests per minute per client   |
| `RANKING_POPULARITY_WEIGHT`  | `2.0`                          | weight of view-count in ranking  |
| `CRAWLER_INTERVAL_MS`        | `3600000`                      | crawl scheduler period           |
| `CRAWLER_REQUEST_DELAY_MS`   | `1000`                         | min delay between requests per host |
| `TRACING_SAMPLE_PROBABILITY` | `0.1`                          | Brave sampling probability       |
| `ZIPKIN_ENDPOINT`            | `http://localhost:9411/api/v2/spans` | trace export URL          |

The `prod` profile (`application-prod.yml`) requires `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `ADMIN_USERNAME`, `ADMIN_PASSWORD` and
`SECURITY_JWT_SECRET`; it fails fast instead of falling back to dev credentials.

Rate limiting: fixed window, `RATE_LIMIT_RPM` requests/minute per client (keyed by
`X-API-Key` header, else client IP), applied to `/api/**`. Over quota returns
`429` with `Retry-After`. In-memory only; back it with Redis for multi-instance.

## Crawling

`SellerCrawler` implementations are selected by `CrawlerFactory`:

| Adapter | When it is used |
|---------|-----------------|
| `HttpSellerCrawlerAdapter` | Seller has a `crawlConfig` (highest precedence) |
| `AmazonCrawlerAdapter` | No config, URL contains `amazon.`; built-in selectors |
| `EbayCrawlerAdapter` | No config, URL contains `ebay.`; built-in selectors |
| `DigikalaCrawlerAdapter` | `isDigikala=true`, no config | fixture catalogue |
| `GenericSellerCrawlerAdapter` | Any other seller without config | fixture catalogue |

Sellers with a `crawlConfig` are scraped for real: Java `HttpClient` fetch, jsoup
CSS selectors (or JSON pointers), pagination via a `{page}` token, robots.txt
enforcement and per-host rate limiting.

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

| Method & path                         | Auth        | Description                       |
|---------------------------------------|-------------|----------------------------------|
| `POST /api/auth/register`             | none        | Create account, returns JWT      |
| `POST /api/auth/login`                | none        | Exchange credentials for JWT     |
| `GET /api/auth/me`                    | JWT / admin | Current principal                |
| `GET /api/search`                     | none        | Full-text search + ranking       |
| `GET /api/products/{id}`              | none        | Detail + price history (emits `product.viewed`) |
| `GET /api/products/{id}/compare`      | none        | Price comparison across sellers  |
| `POST /api/products`                  | admin       | Register a product               |
| `POST /api/products/{id}/price`       | admin       | Upsert a seller price            |
| `DELETE /api/products/{id}`           | admin       | Delete a product and its prices  |
| `GET /api/sellers`                    | none        | List crawler targets             |
| `POST` / `DELETE /api/sellers`        | admin       | Manage crawler targets           |
| `POST /api/price-alerts`              | user/admin  | Create a price-drop alert        |
| `GET /api/price-alerts/me`            | user/admin  | Current user's alerts            |
| `GET /api/price-alerts/{email}`       | self/admin  | List alerts for an e-mail        |
| `DELETE /api/price-alerts/{id}`       | self/admin  | Deactivate an alert              |

Swagger UI documents every endpoint with request/response examples and both
security schemes (`bearerAuth`, `basicAuth`).

## Observability

- `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`
- `/actuator/metrics` and `/actuator/prometheus`
- Custom metrics: `pse.search.latency`, `pse.search.results` (cache hit/miss),
  `pse.crawl.duration`, `pse.crawl.items`, `pse.alerts.fired`
- Brave tracing exported to Zipkin; trace/span ids are added to every log line
- Structured JSON logs when `SPRING_PROFILES_ACTIVE=prod`
- Grafana dashboard `PSE / Product Search Engine - Overview` is auto-provisioned

## Deployment

- Multi-stage, non-root `Dockerfile` with a liveness healthcheck and `.dockerignore`
- `deploy/k8s/` contains namespace, configmap, secret example, deployment,
  service, ingress and HPA; render with `kubectl apply -k deploy/k8s`
- Secrets are never baked into images; see `deploy/k8s/secret.example.yaml`

## Tests

```bash
mvn test      # fast unit + controller + security tests, integration tag excluded
mvn verify    # + Testcontainers integration tests (auto-skipped without Docker)
```

Coverage now includes service unit tests, crawler adapters, the crawler
scheduler, REST controller contracts, JWT/auth behaviour, the Spring Security
filter chain (public/admin/user/JWT edge cases), fuzzy matching, price
normalisation and a full Testcontainers end-to-end flow.

## Known limitations / next steps

- Built-in Amazon/eBay selectors are best-effort and can change; use a per-seller
  `crawlConfig` or an official marketplace API for production reliability.
- No JS rendering: crawlers handle static HTML or JSON endpoints only.
- The rate limiter is in-memory; use Redis or an API gateway for multi-instance.
- Elasticsearch uses the default analyzer (no language-specific analysis).
- JWT is symmetric HS256; move to an OIDC provider / rotating asymmetric keys for
  multi-service deployments.
- Price-alert delivery is e-mail only; add push/SMS channels if needed.
