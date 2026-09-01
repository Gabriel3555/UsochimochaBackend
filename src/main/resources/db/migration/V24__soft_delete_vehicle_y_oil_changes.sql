-- Habilita soft-delete (mismo patrón que refueling_records.status) para poder
-- editar/eliminar cambios de aceite ya registrados desde el Consolidado, "en
-- caso de error" — hoy estas dos tablas no tienen ninguna forma de corregir un
-- registro histórico, solo crear uno nuevo.
ALTER TABLE vehicle_oil_changes ADD COLUMN status BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE oil_changes ADD COLUMN status BOOLEAN NOT NULL DEFAULT TRUE;
