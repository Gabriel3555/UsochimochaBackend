-- Migración: Tarjeta de propiedad en vehículos + Licencia de tránsito en usuarios
-- Fecha: 2026-06-02

-- 1. Usuarios: renombrar license_number -> license_category, agregar license_document_url
ALTER TABLE users RENAME COLUMN license_number TO license_category;
ALTER TABLE users ADD COLUMN IF NOT EXISTS license_document_url VARCHAR(1024);

-- 2. Documentos de vehículos: permitir NULL en fecha_vencimiento (para TARJETA DE PROPIEDAD)
ALTER TABLE documentacion_y_elementos ALTER COLUMN fecha_vencimiento DROP NOT NULL;

-- 3. Desactivar registros anteriores de LICENCIA DE CONDUCCION en vehículos
UPDATE documentacion_y_elementos
SET activo = false
WHERE tipo_documento = 'LICENCIA DE CONDUCCION';
