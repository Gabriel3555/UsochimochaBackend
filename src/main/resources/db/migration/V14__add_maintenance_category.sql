-- Add maintenance category column to orders table
ALTER TABLE orders ADD COLUMN IF NOT EXISTS maintenance_category VARCHAR(30);

COMMENT ON COLUMN orders.maintenance_category IS 'Type of maintenance approach: PREVENTIVO (scheduled) or CORRECTIVO (reactive)';
