CREATE TABLE price_history (
    id          BIGSERIAL PRIMARY KEY,
    product_id  VARCHAR(50) NOT NULL,
    seller_id   BIGINT NOT NULL,
    price       NUMERIC(19, 2) NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_price_history_product ON price_history (product_id, recorded_at DESC);