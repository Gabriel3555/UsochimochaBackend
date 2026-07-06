-- ============================================
-- V16: Rellenar oil_type en cambios de aceite de maquinaria existentes
--
-- El consolidado (OilChangeRepository) filtra por oil_type = 'MOTOR' / 'HYDRAULIC',
-- pero los registros creados por el flujo móvil de Mantenimiento solo llenaban
-- los booleanos motor_oil / hydraulic_oil y dejaban oil_type en NULL,
-- por lo que quedaban invisibles para el front.
--
-- Solo se corrigen registros completos (con horómetro e intervalo), que son
-- los que el consolidado puede mostrar sin error.
-- ============================================

UPDATE oil_changes
SET oil_type = 'MOTOR'
WHERE motor_oil = TRUE
  AND (oil_type IS NULL OR oil_type NOT IN ('MOTOR', 'HYDRAULIC'))
  AND hour_meter IS NOT NULL
  AND average_hours_change IS NOT NULL;

UPDATE oil_changes
SET oil_type = 'HYDRAULIC'
WHERE hydraulic_oil = TRUE
  AND (oil_type IS NULL OR oil_type NOT IN ('MOTOR', 'HYDRAULIC'))
  AND hour_meter IS NOT NULL
  AND average_hours_change IS NOT NULL;
