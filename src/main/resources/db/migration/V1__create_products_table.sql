CREATE TABLE products (
    id          VARCHAR(50) PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    brand       VARCHAR(100),
    category    VARCHAR(30) NOT NULL,
    image_url   VARCHAR(500),
    description VARCHAR(2000),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_category ON products (category);