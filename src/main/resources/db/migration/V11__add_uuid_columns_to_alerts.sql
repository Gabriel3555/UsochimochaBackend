-- Add UUID columns to alerts and oil_change_alerts tables for tracking alert instances
-- Enables deletion by UUID from WebSocket-generated alerts

-- Add uuid column to alerts table
ALTER TABLE alerts
ADD COLUMN uuid VARCHAR(36) UNIQUE DEFAULT NULL;

-- Add uuid column to oil_change_alerts table
ALTER TABLE oil_change_alerts
ADD COLUMN uuid VARCHAR(36) UNIQUE DEFAULT NULL;

-- Create indexes for uuid columns
CREATE INDEX idx_alerts_uuid ON alerts(uuid);
CREATE INDEX idx_oil_alerts_uuid ON oil_change_alerts(uuid);
