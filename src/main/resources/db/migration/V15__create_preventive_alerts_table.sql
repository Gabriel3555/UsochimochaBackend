-- V15: Crear tabla de alertas preventivas (FASE 2 - Sistema nuevo)
-- Almacena alertas de:
-- 1. Documentos (SOAT, TECNOMECANICA, EXTINTOR, RUNT)
-- 2. Cambio de aceite vehículos (por kilometraje)
-- 3. Cambio de aceite maquinaria (por horómetro)

CREATE TABLE preventive_alerts (
    id BIGSERIAL PRIMARY KEY,
    asset_id VARCHAR(50) NOT NULL,
    asset_type VARCHAR(20) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    alert_subtype VARCHAR(50),
    color_estado VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    descripcion TEXT,
    fecha_vencimiento DATE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    metric_type VARCHAR(20),
    metric_value DECIMAL(10,2),
    percentage_used DECIMAL(5,2),
    status BOOLEAN DEFAULT true,
    CONSTRAINT check_color CHECK (color_estado IN ('ROJO', 'AMARILLO', 'VERDE')),
    CONSTRAINT check_estado CHECK (estado IN ('ACTIVA', 'RESUELTA'))
);

-- Índices (específicos para preventive_alerts)
CREATE INDEX idx_preventive_asset_id ON preventive_alerts(asset_id);
CREATE INDEX idx_preventive_asset_type ON preventive_alerts(asset_type);
CREATE INDEX idx_preventive_alert_type ON preventive_alerts(alert_type);
CREATE INDEX idx_preventive_color_estado ON preventive_alerts(color_estado);
CREATE INDEX idx_preventive_estado ON preventive_alerts(estado);
CREATE INDEX idx_preventive_fecha_vencimiento ON preventive_alerts(fecha_vencimiento);
CREATE INDEX idx_preventive_asset_type_estado ON preventive_alerts(asset_type, estado);

-- Índice único para alertas activas: una alerta por activo/tipo
CREATE UNIQUE INDEX idx_preventive_active_alert_unique
  ON preventive_alerts(asset_id, alert_type, alert_subtype, estado)
  WHERE status = true;
