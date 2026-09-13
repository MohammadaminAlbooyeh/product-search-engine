-- Optional per-seller crawl configuration (JSON). When present, the HTTP/HTML
-- crawler adapter is used instead of the fixture adapters.
ALTER TABLE sellers ADD COLUMN crawl_config TEXT;
