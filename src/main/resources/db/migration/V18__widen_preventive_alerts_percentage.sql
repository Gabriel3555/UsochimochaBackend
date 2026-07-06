-- ============================================
-- V18: Ampliar percentage_used en preventive_alerts para evitar overflow
--
-- La columna era DECIMAL(5,2) (máximo 999.99). Un activo con un intervalo de
-- cambio mal configurado (ej. muy pequeño) puede calcular un % de uso muy por
-- encima de eso, y el INSERT/UPDATE fallaba con "numeric field overflow".
-- Esa excepción quedaba atrapada por el catch de PreventiveAlertCalculationService
-- pero ya había marcado la transacción como rollback-only, tumbando el lote
-- completo de alertas de ese ciclo (UnexpectedRollbackException).
-- ============================================

ALTER TABLE preventive_alerts ALTER COLUMN percentage_used TYPE DECIMAL(10,2);
