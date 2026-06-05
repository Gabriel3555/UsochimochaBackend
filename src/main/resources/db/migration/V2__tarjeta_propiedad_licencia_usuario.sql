-- ============================================================
-- V2 — Tarjeta de propiedad en vehículos + licencia en usuarios
-- ============================================================

-- 1. fecha_vencimiento pasa a nullable (necesario para TARJETA DE PROPIEDAD)
ALTER TABLE documentacion_y_elementos
    ALTER COLUMN fecha_vencimiento DROP NOT NULL;

-- 2. Manejar license_number → license_category según estado real de la BD:
--    a) Solo license_number existe       → renombrar
--    b) Ambas columnas existen           → eliminar la vieja (ddl-auto=update ya creó la nueva)
--    c) Solo license_category existe     → nada que hacer
DO $$
BEGIN
    IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'users' AND column_name = 'license_number'
        )
    THEN
        IF EXISTS (
                SELECT 1 FROM information_schema.columns
                WHERE table_name = 'users' AND column_name = 'license_category'
            )
        THEN
            -- Ambas existen: la nueva ya fue creada por Hibernate, eliminar la vieja
            ALTER TABLE users DROP COLUMN license_number;
        ELSE
            -- Solo existe la vieja: renombrar
            ALTER TABLE users RENAME COLUMN license_number TO license_category;
        END IF;
    END IF;
END$$;

-- 3. Agregar columna de documento de licencia (si no existe)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS license_document_url VARCHAR(1024);

-- 4. Desactivar registros viejos de LICENCIA DE CONDUCCION en vehículos
UPDATE documentacion_y_elementos
    SET activo = false
    WHERE tipo_documento = 'LICENCIA DE CONDUCCION';
