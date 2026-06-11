-- ============================================================
-- V6 — Crear tabla de alertas preventivas
-- ============================================================
-- Tabla que almacena alertas preventivas calculadas basadas
-- en cambios de aceite, documentos próximos a vencer, licencias, etc.
-- Los datos se consultan de tablas existentes, esta tabla solo
-- almacena las alertas generadas y su estado.

CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,

    -- Identificación de la alerta
    placa VARCHAR(20) NOT NULL,              -- Placa del vehículo/moto o ID máquina
    tipo_alerta VARCHAR(50) NOT NULL,        -- CAMBIO_ACEITE, DOCUMENTO, LICENCIA, MANTENIMIENTO

    -- Descripción y detalles
    descripcion VARCHAR(500),                -- Descripción legible: "SOAT próximo a vencer", etc.

    -- Fechas clave
    fecha_vencimiento DATE,                  -- Cuándo vence el documento/cambio recomendado
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Cuándo se creó la alerta

    -- Estado de la alerta
    estado VARCHAR(20) NOT NULL,             -- ACTIVE = alerta activa, RESOLVED = resuelta
    color_estado VARCHAR(20),                -- ROJO (crítica/vencida), AMARILLO (warning), VERDE (ok)

    -- Auditoría
    created_by_user_id BIGINT,               -- Quién/qué creó la alerta (puede ser sistema)

    -- Referencias para trazabilidad (opcional)
    documento_id BIGINT,                     -- ID del documento si es alerta de documento

    -- Restricción: usuario que creó debe existir
    CONSTRAINT fk_alerts_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
);

-- ============================================================
-- Crear índices para búsquedas frecuentes (PostgreSQL syntax)
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_alerts_placa ON alerts(placa);
CREATE INDEX IF NOT EXISTS idx_alerts_estado ON alerts(estado);
CREATE INDEX IF NOT EXISTS idx_alerts_tipo_alerta ON alerts(tipo_alerta);
CREATE INDEX IF NOT EXISTS idx_alerts_fecha_vencimiento ON alerts(fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_alerts_placa_tipo_estado ON alerts(placa, tipo_alerta, estado);

-- ============================================================
-- Notas de implementación:
-- ============================================================
--
-- 1. PLACA: Puede ser:
--    - Placa de vehículo (ABC-123)
--    - Placa de moto (ABC-456)
--    - ID de máquina (MAQUINA-001)
--
-- 2. TIPO_ALERTA: Tipos definidos:
--    - CAMBIO_ACEITE: Vehículo/máquina necesita cambio de aceite
--    - DOCUMENTO: Documento próximo a vencer (SOAT, RUNT, etc.)
--    - LICENCIA: Licencia de usuario próxima a vencer
--    - MANTENIMIENTO: Mantenimiento programado próximo
--
-- 3. ESTADO:
--    - ACTIVE: Alerta actualmente vigente (no resuelta)
--    - RESOLVED: Alerta resuelta por el usuario
--
-- 4. COLOR_ESTADO (calculado):
--    - ROJO: fecha_vencimiento <= HOY (crítica, vencida)
--    - AMARILLO: fecha_vencimiento <= HOY + 30 días (warning)
--    - VERDE: fecha_vencimiento > HOY + 30 días (ok, no se crea)
--
-- 5. CREATED_BY_USER_ID:
--    - Puede ser NULL si fue creada por el sistema
--    - Si fue creada manualmente, tiene el ID del usuario
--
-- 6. DOCUMENTO_ID:
--    - Si la alerta es sobre un documento específico,
--      aquí va el ID de documentacion_y_elementos
--
-- ============================================================
