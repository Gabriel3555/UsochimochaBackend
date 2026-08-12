-- La unidad de consumo de maquinaria estaba en formato "tasa" (galones/m³
-- consumidos por hora: GAL_POR_HORA, M3_POR_HORA), inconsistente con la de
-- vehículos/motos (KM_POR_GALON, KM_POR_M3), que siempre fue formato
-- "rendimiento" (cuánto rinde cada unidad de combustible). Se homologan las 4
-- unidades al formato rendimiento — horas que rinde cada galón/m³ — para que
-- FuelPerformanceService use una sola fórmula (dividir) en vez de una rama
-- distinta para maquinaria.
--
-- El número configurado cambia de significado (se invierte): "0.5" bajo
-- GAL_POR_HORA significaba "gasta 0.5 gal cada hora" (= 2 horas por galón);
-- bajo HORA_POR_GALON ese mismo rendimiento se guarda como "2".
--
-- Orden importa: el CHECK se quita ANTES de tocar los datos (si no, el UPDATE
-- de abajo intenta guardar 'HORA_POR_GALON' mientras aún rige la restricción
-- vieja) y se vuelve a agregar AL FINAL, ya con todas las filas migradas — un
-- ADD CONSTRAINT a mitad de camino habría fallado igual, porque Postgres valida
-- el CHECK contra las filas viejas que todavía no se habían actualizado.
ALTER TABLE asset_fuel_config DROP CONSTRAINT asset_fuel_config_unidad_consumo_check;

UPDATE asset_fuel_config
SET unidad_consumo = 'HORA_POR_GALON', consumo_estandar = 1 / consumo_estandar
WHERE unidad_consumo = 'GAL_POR_HORA';

UPDATE asset_fuel_config
SET unidad_consumo = 'HORA_POR_M3', consumo_estandar = 1 / consumo_estandar
WHERE unidad_consumo = 'M3_POR_HORA';

ALTER TABLE asset_fuel_config ADD CONSTRAINT asset_fuel_config_unidad_consumo_check
    CHECK (unidad_consumo IN ('HORA_POR_GALON', 'KM_POR_GALON', 'HORA_POR_M3', 'KM_POR_M3'));
