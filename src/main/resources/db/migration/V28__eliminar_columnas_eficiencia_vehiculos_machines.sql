-- ============================================================
-- V28 — Elimina de nuevo fuel_tank_capacity_gallons / factory_efficiency_*
-- en vehiculos y machines.
--
-- V19 ya las había eliminado al mover ese dato a asset_fuel_config, pero
-- VehicleEntity/MachineEntity (PR #52, commit 67c2256) las reintrodujeron
-- por un merge con una rama vieja, causando el incidente de producción
-- del 2026-09-02 (columnas referenciadas por Hibernate que no existían
-- en la BD). Se restauraron temporalmente vía hotfix manual para levantar
-- el servicio; esta migración limpia el esquema definitivamente ahora que
-- el código (VehicleEntity/MachineEntity/DTOs/servicios) dejó de usarlas
-- — el módulo de combustible real y activo es asset_fuel_config.
-- ============================================================

ALTER TABLE vehiculos DROP COLUMN IF EXISTS fuel_tank_capacity_gallons;
ALTER TABLE vehiculos DROP COLUMN IF EXISTS factory_efficiency_km_per_gallon;
ALTER TABLE vehiculos DROP COLUMN IF EXISTS factory_efficiency_unit;

ALTER TABLE machines DROP COLUMN IF EXISTS fuel_tank_capacity_gallons;
ALTER TABLE machines DROP COLUMN IF EXISTS factory_efficiency_gal_per_hour;
ALTER TABLE machines DROP COLUMN IF EXISTS factory_efficiency_unit;
