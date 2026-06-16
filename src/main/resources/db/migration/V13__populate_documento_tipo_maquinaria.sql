-- Populate tipo_maquinaria for document alerts
-- Set default value for existing document alerts
UPDATE alerts
SET tipo_maquinaria = 'DOCUMENTO'
WHERE tipo_alerta LIKE 'DOCUMENTO_%'
  AND tipo_maquinaria IS NULL;
