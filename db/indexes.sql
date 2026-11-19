-- Indexes added on top of the provided schema (database.sql).
-- Portfolio and order placement filter orders by user and status.
CREATE INDEX IF NOT EXISTS idx_orders_userid_status ON orders (userid, status);
-- Lookup of the latest marketdata by instrument.
CREATE INDEX IF NOT EXISTS idx_marketdata_instrumentid_date ON marketdata (instrumentid, date);
