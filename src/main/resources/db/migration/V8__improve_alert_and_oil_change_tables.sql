-- Mejorar tabla alerts con campos para alertas de cambio de aceite
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS metric VARCHAR(20);
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;

-- Mejorar tabla vehicle_oil_changes con referencias a requisitos
ALTER TABLE vehicle_oil_changes ADD COLUMN IF NOT EXISTS id_requirement BIGINT;
ALTER TABLE vehicle_oil_changes ADD COLUMN IF NOT EXISTS percentage_used INT DEFAULT 0;
ALTER TABLE vehicle_oil_changes ADD COLUMN IF NOT EXISTS placa VARCHAR(20);

-- Mejorar tabla oil_changes con campos mejorados
ALTER TABLE oil_changes ADD COLUMN IF NOT EXISTS hour_stamp INT;
ALTER TABLE oil_changes ADD COLUMN IF NOT EXISTS id_requirement BIGINT;
ALTER TABLE oil_changes ADD COLUMN IF NOT EXISTS percentage_used INT DEFAULT 0;

-- Crear índices para mejor rendimiento
CREATE INDEX IF NOT EXISTS idx_vehicle_oil_changes_placa ON vehicle_oil_changes(placa);
CREATE INDEX IF NOT EXISTS idx_vehicle_oil_changes_requirement ON vehicle_oil_changes(id_requirement);
CREATE INDEX IF NOT EXISTS idx_oil_changes_hour_stamp ON oil_changes(hour_stamp);
CREATE INDEX IF NOT EXISTS idx_oil_changes_requirement ON oil_changes(id_requirement);
