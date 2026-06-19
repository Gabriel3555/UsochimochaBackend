-- Add maintenance category column to orders table
ALTER TABLE orders ADD COLUMN maintenance_category VARCHAR(30);

-- Add comment explaining the field
COMMENT ON COLUMN orders.maintenance_category IS 'Type of maintenance approach: PREVENTIVO (scheduled) or CORRECTIVO (reactive)';
