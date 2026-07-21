-- ============================================
-- V17: Rellenar hour_stamp en cambios de aceite de maquinaria existentes
--
-- El sistema de alertas preventivas (PreventiveAlertCalculationService,
-- MachineMonitoringService) calcula el estado del aceite a partir de
-- hour_stamp, pero el flujo de registro (OilChangeMapper) solo llenaba
-- hour_meter, dejando hour_stamp en NULL. Como consecuencia, las máquinas
-- con cambios de aceite ya registrados seguían generando la alerta
-- "PRIMER CAMBIO DE ACEITE RECOMENDADO" indefinidamente.
--
-- OilChangeMapper ya fue corregido para llenar ambos campos en los
-- registros nuevos; esta migración corrige los registros históricos.
-- ============================================

UPDATE oil_changes
SET hour_stamp = ROUND(hour_meter)::INTEGER
WHERE hour_stamp IS NULL
  AND hour_meter IS NOT NULL;
