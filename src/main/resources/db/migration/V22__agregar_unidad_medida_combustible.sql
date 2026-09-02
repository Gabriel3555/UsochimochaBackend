-- El gas natural vehicular se mide en m³, no en galones — hasta ahora todo el módulo
-- de combustibles asumía galones para cualquier tipo. Este cambio hace explícita la
-- unidad de cada tipo de combustible en el catálogo, y extiende las unidades de
-- consumo estándar (rendimiento operativo) para que también acepten m³.
ALTER TABLE fuel_types
  ADD COLUMN unidad_medida VARCHAR(10) NOT NULL DEFAULT 'GALON'
  CHECK (unidad_medida IN ('GALON', 'M3'));

UPDATE fuel_types SET unidad_medida = 'M3' WHERE codigo = 'GAS';

ALTER TABLE asset_fuel_config DROP CONSTRAINT asset_fuel_config_unidad_consumo_check;
ALTER TABLE asset_fuel_config
  ADD CONSTRAINT asset_fuel_config_unidad_consumo_check
  CHECK (unidad_consumo IN ('GAL_POR_HORA', 'KM_POR_GALON', 'M3_POR_HORA', 'KM_POR_M3'));
