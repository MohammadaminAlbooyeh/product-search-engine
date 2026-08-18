CREATE TABLE price_entries (
    id           BIGSERIAL PRIMARY KEY,
    product_id   VARCHAR(50) NOT NULL REFERENCES products (id),
    seller_id    BIGINT NOT NULL REFERENCES sellers (id),
    price        NUMERIC(19, 2) NOT NULL,
    url          VARCHAR(500),
    availability VARCHAR(20) NOT NULL,
    fetched_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_price_entry_product_seller UNIQUE (product_id, seller_id)
);

CREATE INDEX idx_price_entries_product ON price_entries (product_id);
CREATE INDEX idx_price_entries_seller ON price_entries (seller_id);