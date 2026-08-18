CREATE TABLE price_alerts (
    id           BIGSERIAL PRIMARY KEY,
    product_id   VARCHAR(50) NOT NULL,
    email        VARCHAR(100) NOT NULL,
    target_price NUMERIC(19, 2) NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_price_alerts_product ON price_alerts (product_id);
CREATE INDEX idx_price_alerts_email ON price_alerts (email);