-- =============================================================================
-- Migración manual: índices y CHECK CONSTRAINT para fuel_logs
-- Ejecutar en la BD de desarrollo/producción una sola vez.
-- Idempotente: usa IF NOT EXISTS donde aplica.
-- =============================================================================

-- Índice compuesto principal: consultas por activo en orden temporal
CREATE INDEX IF NOT EXISTS idx_fuel_logs_asset
    ON fuel_logs (asset_type, asset_id, fuel_date_time DESC);

-- Índice para listados y exportación por fecha
CREATE INDEX IF NOT EXISTS idx_fuel_logs_date
    ON fuel_logs (fuel_date_time DESC);

-- Índice parcial: solo registros anómalos (queries de anomalías son rápidas)
CREATE INDEX IF NOT EXISTS idx_fuel_logs_anomaly
    ON fuel_logs (is_anomaly) WHERE is_anomaly = TRUE;

-- Índice parcial: solo facturas pendientes de revisión
CREATE INDEX IF NOT EXISTS idx_fuel_logs_invoice
    ON fuel_logs (invoice_status) WHERE invoice_status = 'PENDING_REVIEW';

-- Índice para la tabla materializada de estadísticas mensuales
CREATE INDEX IF NOT EXISTS idx_fuel_stats_month
    ON fuel_stats_monthly (year_month, asset_type);

-- =============================================================================
-- CHECK CONSTRAINT: garantiza que al menos un medidor esté presente según el tipo
-- Ejecutar solo si no existe — verificar previamente con:
--   SELECT conname FROM pg_constraint WHERE conname = 'chk_fuel_meter';
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_fuel_meter'
    ) THEN
        ALTER TABLE fuel_logs
        ADD CONSTRAINT chk_fuel_meter CHECK (
            (asset_type IN ('VEHICLE', 'MOTO') AND odometer_km IS NOT NULL)
            OR (asset_type = 'MACHINE' AND hour_meter IS NOT NULL)
            OR (odometer_km IS NOT NULL AND hour_meter IS NOT NULL)
        );
    END IF;
END $$;

-- CHECK CONSTRAINT: unidad de cantidad válida (incluye CUBIC_METERS para gas natural)
DO $$
BEGIN
    -- Eliminar constraint anterior si solo permitía LITERS/GALLONS
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_quantity_unit'
    ) THEN
        ALTER TABLE fuel_logs DROP CONSTRAINT chk_quantity_unit;
    END IF;

    ALTER TABLE fuel_logs
    ADD CONSTRAINT chk_quantity_unit CHECK (
        quantity_unit IN ('LITERS', 'GALLONS', 'CUBIC_METERS')
    );
END $$;
