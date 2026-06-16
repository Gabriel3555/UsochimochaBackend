-- Add tipo_maquinaria column to alerts table to store vehicle/machine/motorcycle type
ALTER TABLE alerts
ADD COLUMN tipo_maquinaria VARCHAR(50) DEFAULT NULL;

-- Create index for filtering alerts by machinery type
CREATE INDEX idx_alerts_tipo_maquinaria ON alerts(tipo_maquinaria);
