-- Add machinery horometro tracking and machine support to alerts
-- V9: Complete implementation for machinery oil change alerts

-- Add horometro_actual column to machines for tracking operating hours
ALTER TABLE machines ADD COLUMN IF NOT EXISTS horometro_actual INT DEFAULT 0;

-- Extend alerts table to support both vehicles (placa) and machines
-- Make placa nullable (vehicles have placa, machines have machine_id)
ALTER TABLE alerts ALTER COLUMN placa DROP NOT NULL;
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS machine_id BIGINT;
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS machine_name VARCHAR(255);
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS subtipo VARCHAR(50);

-- Create indexes for machinery-related queries
CREATE INDEX IF NOT EXISTS idx_machines_horometro_actual ON machines(horometro_actual);
CREATE INDEX IF NOT EXISTS idx_alerts_machine_id ON alerts(machine_id);
CREATE INDEX IF NOT EXISTS idx_alerts_machine_tipo_subtipo ON alerts(machine_id, tipo_alerta, subtipo);
