-- ============================================================
-- V19 — Retiro completo del módulo de combustibles (BD)
-- El módulo se reconstruye desde cero (backend, frontend y móvil).
-- Esta migración elimina las tablas propias del módulo y las
-- columnas que se habían agregado a machines/vehiculos únicamente
-- para su uso (capacidad de tanque y eficiencia de fábrica).
-- ============================================================

DROP TABLE IF EXISTS fuel_asset_config;
DROP TABLE IF EXISTS fuel_stats_monthly;
DROP TABLE IF EXISTS fuel_logs;
DROP TABLE IF EXISTS fuel_stations;

ALTER TABLE vehiculos DROP COLUMN IF EXISTS fuel_tank_capacity_gallons;
ALTER TABLE vehiculos DROP COLUMN IF EXISTS factory_efficiency_km_per_gallon;
ALTER TABLE vehiculos DROP COLUMN IF EXISTS factory_efficiency_unit;

ALTER TABLE machines DROP COLUMN IF EXISTS fuel_tank_capacity_gallons;
ALTER TABLE machines DROP COLUMN IF EXISTS factory_efficiency_gal_per_hour;
ALTER TABLE machines DROP COLUMN IF EXISTS factory_efficiency_unit;
