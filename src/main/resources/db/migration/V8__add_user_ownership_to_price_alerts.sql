ALTER TABLE price_alerts ADD COLUMN user_id BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE price_alerts ALTER COLUMN email TYPE VARCHAR(150);

CREATE INDEX idx_price_alerts_user ON price_alerts (user_id);
