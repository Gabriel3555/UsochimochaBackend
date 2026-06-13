-- V10: Crear tabla de auditoría para alertas de cambio de aceite (Fase 3)
--
-- Registra cada alerta de cambio de aceite enviada en tiempo real.
-- Útil para: auditoría, análisis de SLA, reporte de notificaciones.

CREATE TABLE IF NOT EXISTS oil_change_alerts (
    id BIGSERIAL PRIMARY KEY,

    -- Identificación de la maquinaria
    placa VARCHAR(20) NOT NULL,
    tipo_maquinaria VARCHAR(50) NOT NULL,  -- "VEHICULO", "MOTOCICLETA", "MAQUINARIA"

    -- Estado de la alerta
    alert_color VARCHAR(20) NOT NULL,      -- "GREEN", "BLUE", "YELLOW", "RED"
    alert_status VARCHAR(100),              -- "OK", "Programado", "Próximo a Cambio", "Cambio de Aceite"
    percentage_used DECIMAL(10, 2),        -- 0-100+ %
    distance_remaining BIGINT,             -- km o horas restantes

    -- Mensaje y contexto
    alert_message TEXT,

    -- Timestamps
    notification_sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Soft delete
    status BOOLEAN DEFAULT TRUE
);

-- Índices para búsquedas frecuentes
CREATE INDEX idx_oil_alerts_placa ON oil_change_alerts(placa);
CREATE INDEX idx_oil_alerts_color ON oil_change_alerts(alert_color);
CREATE INDEX idx_oil_alerts_tipo ON oil_change_alerts(tipo_maquinaria);
CREATE INDEX idx_oil_alerts_timestamp ON oil_change_alerts(created_at DESC);
CREATE INDEX idx_oil_alerts_status ON oil_change_alerts(status);

-- Comentarios
COMMENT ON TABLE oil_change_alerts IS 'Auditoría de alertas de cambio de aceite enviadas en tiempo real';
COMMENT ON COLUMN oil_change_alerts.placa IS 'Identificador del vehículo/moto/máquina';
COMMENT ON COLUMN oil_change_alerts.alert_color IS 'Color visual de la alerta: GREEN (OK), BLUE (Programado), YELLOW (Próximo), RED (Urgente)';
COMMENT ON COLUMN oil_change_alerts.percentage_used IS 'Porcentaje del intervalo de cambio utilizado';
COMMENT ON COLUMN oil_change_alerts.notification_sent_at IS 'Cuándo se envió la notificación por WebSocket';
