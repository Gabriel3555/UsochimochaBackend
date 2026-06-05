-- Insertar tipo de vehículo MOTOCICLETA si no existe
-- Idempotente: sin errores si ya existe
INSERT INTO cat_tipos_vehiculo (nombre_tipo, activo)
SELECT 'MOTOCICLETA', true
WHERE NOT EXISTS (
    SELECT 1 FROM cat_tipos_vehiculo WHERE nombre_tipo = 'MOTOCICLETA'
);
