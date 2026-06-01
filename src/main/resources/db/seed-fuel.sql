-- =============================================================================
-- Seed de combustible — Usochicamocha
-- Ejecutar: docker exec -i usochicamocha_local psql -U postgres -d postgres < seed-fuel.sql
-- Idempotente: omite si ya hay >= 30 registros en fuel_logs.
-- =============================================================================

DO $$
DECLARE
  v_maq1 BIGINT;
  v_maq2 BIGINT;
  v_maq3 BIGINT;
BEGIN
  IF (SELECT COUNT(*) FROM fuel_logs) >= 30 THEN
    RAISE NOTICE 'Seed omitido: ya existen suficientes registros de combustible.';
    RETURN;
  END IF;

  -- ── 1. Insertar máquinas y capturar IDs ──────────────────────────────────
  INSERT INTO machines (name, model, brand, status, belongs_to, num_inter_identification)
  VALUES ('Excavadora hidráulica', 'CAT 320D', 'Caterpillar', true, 'Usochicamocha', 'USO-CAT320D')
  RETURNING id INTO v_maq1;

  INSERT INTO machines (name, model, brand, status, belongs_to, num_inter_identification)
  VALUES ('Buldócer', 'D6N XL', 'Caterpillar', true, 'Usochicamocha', 'USO-D6NXL')
  RETURNING id INTO v_maq2;

  INSERT INTO machines (name, model, brand, status, belongs_to, num_inter_identification)
  VALUES ('Retroexcavadora', '3CX PRO', 'JCB', true, 'Usochicamocha', 'USO-3CXPRO')
  RETURNING id INTO v_maq3;

  RAISE NOTICE 'Máquinas creadas: MAQ1=%, MAQ2=%, MAQ3=%', v_maq1, v_maq2, v_maq3;

  -- ── 2. Insertar cargas de combustible ─────────────────────────────────────
  -- quantity en galones | quantity_liters = quantity * 3.785411784
  -- Precios COP/galón: ACPM ~$12.200-12.500, Gasolina ~$15.800-16.200

  INSERT INTO fuel_logs (
    asset_type, asset_id, asset_plate, fuel_date_time,
    odometer_km, hour_meter, previous_odometer, previous_hour_meter, delta_km, delta_hours,
    quantity, quantity_unit, quantity_liters,
    price_per_unit, total_cost_calculated, total_cost_actual, total_cost_mismatch,
    fuel_type, service_station, is_full_tank, is_anomaly,
    efficiency_value, efficiency_unit,
    invoice_status, registered_by, created_at
  ) VALUES

  -- ══════════════ DICIEMBRE 2025 ══════════════

  -- Excavadora
  ('MACHINE', v_maq1, 'MAQ-001', '2025-12-10 08:30:00',
   NULL, 3330, NULL, 3200, NULL, 130,
   53.0, 'GALLONS', 200.627, 12200, 646600, NULL, false,
   'ACPM', 'Terpel', true, false, 3.80, 'GALLON_PER_HOUR',
   'PENDING_REVIEW', 'Carlos R.', '2025-12-10 08:30:00'),

  ('MACHINE', v_maq1, 'MAQ-001', '2025-12-25 14:00:00',
   NULL, 3430, NULL, 3330, NULL, 100,
   38.0, 'GALLONS', 143.846, 12100, 459800, NULL, false,
   'ACPM', 'Biomax', false, false, NULL, NULL,
   'PENDING_REVIEW', 'Luis M.', '2025-12-25 14:00:00'),

  -- Buldócer
  ('MACHINE', v_maq2, 'MAQ-002', '2025-12-12 09:00:00',
   NULL, 1930, NULL, 1800, NULL, 130,
   61.0, 'GALLONS', 230.920, 12350, 753350, NULL, false,
   'ACPM', 'Primax', true, false, 3.80, 'GALLON_PER_HOUR',
   'PENDING_REVIEW', 'Luis M.', '2025-12-12 09:00:00'),

  ('MACHINE', v_maq2, 'MAQ-002', '2025-12-26 15:00:00',
   NULL, 2045, NULL, 1930, NULL, 115,
   43.0, 'GALLONS', 162.771, 12250, 526750, NULL, false,
   'ACPM', 'Texaco', false, false, NULL, NULL,
   'PENDING_REVIEW', 'Ana G.', '2025-12-26 15:00:00'),

  -- Vehículos
  ('VEHICLE', 1, 'OGD742', '2025-12-05 07:15:00',
   45680, NULL, 45000, NULL, 680, NULL,
   12.0, 'GALLONS', 45.425, 15800, 189600, NULL, false,
   'GASOLINA_EXTRA', 'Terpel', true, false, 13.50, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Pedro T.', '2025-12-05 07:15:00'),

  ('VEHICLE', 2, 'MIF301', '2025-12-08 07:30:00',
   72720, NULL, 72000, NULL, 720, NULL,
   10.0, 'GALLONS', 37.854, 15800, 158000, NULL, false,
   'GASOLINA_CORRIENTE', 'Biomax', true, false, 14.20, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Carlos R.', '2025-12-08 07:30:00'),

  -- KBH055: discrepancia + anomalía de consumo
  ('VEHICLE', 3, 'KBH055', '2025-12-11 07:45:00',
   32170, NULL, 31500, NULL, 670, NULL,
   14.0, 'GALLONS', 52.996, 15880, 222320, 215500, true,
   'GASOLINA_CORRIENTE', 'Primax', true, true, 4.20, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Ana G.', '2025-12-11 07:45:00'),

  ('VEHICLE', 4, 'NCP187', '2025-12-14 08:00:00',
   98740, NULL, 98000, NULL, 740, NULL,
   11.0, 'GALLONS', 41.639, 15750, 173250, NULL, false,
   'GASOLINA_CORRIENTE', 'Terpel', true, false, 12.50, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Luis M.', '2025-12-14 08:00:00'),

  -- Motos
  ('MOTO', 11, 'XLB481', '2025-12-15 12:00:00',
   12380, NULL, 12000, NULL, 380, NULL,
   1.5, 'GALLONS', 5.678, 15600, 23400, NULL, false,
   'GASOLINA_CORRIENTE', 'Terpel', true, false, NULL, NULL,
   'PENDING_REVIEW', 'Ana G.', '2025-12-15 12:00:00'),

  ('MOTO', 12, 'CFR076', '2025-12-18 12:00:00',
   8900, NULL, 8500, NULL, 400, NULL,
   1.9, 'GALLONS', 7.192, 15660, 29754, NULL, false,
   'GASOLINA_CORRIENTE', 'Biomax', true, false, NULL, NULL,
   'PENDING_REVIEW', 'Pedro T.', '2025-12-18 12:00:00'),

  -- ══════════════ ENERO 2026 ══════════════

  ('MACHINE', v_maq1, 'MAQ-001', '2026-01-10 08:30:00',
   NULL, 3560, NULL, 3430, NULL, 130,
   58.0, 'GALLONS', 219.553, 12300, 713400, NULL, false,
   'ACPM', 'Terpel', true, false, 3.80, 'GALLON_PER_HOUR',
   'PENDING_REVIEW', 'Carlos R.', '2026-01-10 08:30:00'),

  ('MACHINE', v_maq2, 'MAQ-002', '2026-01-12 09:00:00',
   NULL, 2175, NULL, 2045, NULL, 130,
   63.0, 'GALLONS', 238.479, 12350, 778050, NULL, false,
   'ACPM', 'Primax', true, false, 3.80, 'GALLON_PER_HOUR',
   'PENDING_REVIEW', 'Luis M.', '2026-01-12 09:00:00'),

  ('VEHICLE', 1, 'OGD742', '2026-01-05 07:15:00',
   46380, NULL, 45680, NULL, 700, NULL,
   12.0, 'GALLONS', 45.425, 15850, 190200, NULL, false,
   'GASOLINA_EXTRA', 'Repsol', true, false, 13.80, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Pedro T.', '2026-01-05 07:15:00'),

  ('VEHICLE', 2, 'MIF301', '2026-01-08 07:30:00',
   73450, NULL, 72720, NULL, 730, NULL,
   10.5, 'GALLONS', 39.746, 15900, 166950, NULL, false,
   'GASOLINA_CORRIENTE', 'Terpel', true, false, 14.50, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Carlos R.', '2026-01-08 07:30:00'),

  ('VEHICLE', 5, 'QHE412', '2026-01-17 08:30:00',
   55820, NULL, 55000, NULL, 820, NULL,
   13.0, 'GALLONS', 49.211, 15800, 205400, NULL, false,
   'GASOLINA_CORRIENTE', 'Biomax', true, false, 13.20, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Ana G.', '2026-01-17 08:30:00'),

  ('MOTO', 11, 'XLB481', '2026-01-15 12:00:00',
   12760, NULL, 12380, NULL, 380, NULL,
   1.5, 'GALLONS', 5.678, 15650, 23475, NULL, false,
   'GASOLINA_CORRIENTE', 'Terpel', true, false, NULL, NULL,
   'PENDING_REVIEW', 'Ana G.', '2026-01-15 12:00:00'),

  -- ══════════════ FEBRERO 2026 ══════════════

  ('MACHINE', v_maq1, 'MAQ-001', '2026-02-10 08:30:00',
   NULL, 3690, NULL, 3560, NULL, 130,
   55.0, 'GALLONS', 208.215, 12250, 673750, NULL, false,
   'ACPM', 'Biomax', true, false, 3.80, 'GALLON_PER_HOUR',
   'PENDING_REVIEW', 'Carlos R.', '2026-02-10 08:30:00'),

  ('MACHINE', v_maq3, 'MAQ-003', '2026-02-14 10:00:00',
   NULL, 4630, NULL, 4500, NULL, 130,
   69.0, 'GALLONS', 261.195, 12400, 855600, NULL, false,
   'ACPM', 'Mobil', true, false, 3.80, 'GALLON_PER_HOUR',
   'PENDING_REVIEW', 'Pedro T.', '2026-02-14 10:00:00'),

  -- KBH055: segunda discrepancia
  ('VEHICLE', 3, 'KBH055', '2026-02-11 07:45:00',
   32870, NULL, 32170, NULL, 700, NULL,
   14.0, 'GALLONS', 52.996, 15880, 222320, 215900, true,
   'GASOLINA_CORRIENTE', 'Primax', true, false, 13.60, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Ana G.', '2026-02-11 07:45:00'),

  ('VEHICLE', 6, 'VLK993', '2026-02-20 08:00:00',
   18700, NULL, 18000, NULL, 700, NULL,
   10.0, 'GALLONS', 37.854, 15780, 157800, NULL, false,
   'GASOLINA_CORRIENTE', 'Repsol', true, false, 13.00, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Luis M.', '2026-02-20 08:00:00'),

  ('MOTO', 12, 'CFR076', '2026-02-18 12:00:00',
   9700, NULL, 8900, NULL, 800, NULL,
   2.1, 'GALLONS', 7.950, 15700, 32970, NULL, false,
   'GASOLINA_CORRIENTE', 'Biomax', true, false, NULL, NULL,
   'PENDING_REVIEW', 'Pedro T.', '2026-02-18 12:00:00'),

  -- ══════════════ MARZO 2026 ══════════════

  ('MACHINE', v_maq1, 'MAQ-001', '2026-03-10 08:30:00',
   NULL, 3820, NULL, 3690, NULL, 130,
   60.0, 'GALLONS', 227.143, 12300, 738000, NULL, false,
   'ACPM', 'Terpel', true, false, 3.80, 'GALLON_PER_HOUR',
   'PENDING_REVIEW', 'Carlos R.', '2026-03-10 08:30:00'),

  ('MACHINE', v_maq2, 'MAQ-002', '2026-03-13 09:00:00',
   NULL, 2440, NULL, 2175, NULL, 265,
   66.0, 'GALLONS', 249.862, 12380, 817080, NULL, false,
   'ACPM', 'Texaco', true, false, 3.80, 'GALLON_PER_HOUR',
   'PENDING_REVIEW', 'Luis M.', '2026-03-13 09:00:00'),

  ('VEHICLE', 1, 'OGD742', '2026-03-05 07:15:00',
   47780, NULL, 46380, NULL, 1400, NULL,
   13.0, 'GALLONS', 49.211, 16000, 208000, NULL, false,
   'GASOLINA_EXTRA', 'Terpel', true, false, 14.10, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Pedro T.', '2026-03-05 07:15:00'),

  ('VEHICLE', 4, 'NCP187', '2026-03-14 08:00:00',
   100220, NULL, 98740, NULL, 1480, NULL,
   12.0, 'GALLONS', 45.425, 15800, 189600, NULL, false,
   'GASOLINA_CORRIENTE', 'Biomax', true, false, 14.30, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Luis M.', '2026-03-14 08:00:00'),

  ('VEHICLE', 7, 'RTA628', '2026-03-23 08:15:00',
   64120, NULL, 63000, NULL, 1120, NULL,
   11.0, 'GALLONS', 41.639, 15820, 174020, NULL, false,
   'GASOLINA_CORRIENTE', 'Repsol', true, false, 13.40, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Ana G.', '2026-03-23 08:15:00'),

  ('MOTO', 13, 'PPZ340', '2026-03-21 12:00:00',
   22180, NULL, 21000, NULL, 1180, NULL,
   2.3, 'GALLONS', 8.707, 15750, 36225, NULL, false,
   'GASOLINA_CORRIENTE', 'Mobil', true, false, NULL, NULL,
   'PENDING_REVIEW', 'Carlos R.', '2026-03-21 12:00:00'),

  -- ══════════════ ABRIL 2026 ══════════════

  ('MACHINE', v_maq1, 'MAQ-001', '2026-04-10 08:30:00',
   NULL, 3950, NULL, 3820, NULL, 130,
   62.0, 'GALLONS', 234.718, 12400, 768800, NULL, false,
   'ACPM', 'Terpel', true, false, 3.80, 'GALLON_PER_HOUR',
   'PENDING_REVIEW', 'Carlos R.', '2026-04-10 08:30:00'),

  ('MACHINE', v_maq3, 'MAQ-003', '2026-04-12 10:00:00',
   NULL, 4895, NULL, 4630, NULL, 265,
   74.0, 'GALLONS', 280.121, 12450, 921300, NULL, false,
   'ACPM', 'Primax', true, false, 3.80, 'GALLON_PER_HOUR',
   'PENDING_REVIEW', 'Pedro T.', '2026-04-12 10:00:00'),

  ('VEHICLE', 2, 'MIF301', '2026-04-08 07:30:00',
   74920, NULL, 73450, NULL, 1470, NULL,
   11.0, 'GALLONS', 41.639, 16100, 177100, NULL, false,
   'GASOLINA_CORRIENTE', 'Terpel', true, false, 15.20, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Carlos R.', '2026-04-08 07:30:00'),

  ('VEHICLE', 5, 'QHE412', '2026-04-17 08:30:00',
   57350, NULL, 55820, NULL, 1530, NULL,
   13.0, 'GALLONS', 49.211, 16000, 208000, NULL, false,
   'GASOLINA_CORRIENTE', 'Biomax', true, false, 13.90, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Ana G.', '2026-04-17 08:30:00'),

  ('VEHICLE', 6, 'VLK993', '2026-04-22 08:00:00',
   20100, NULL, 18700, NULL, 1400, NULL,
   10.0, 'GALLONS', 37.854, 15900, 159000, NULL, false,
   'GASOLINA_CORRIENTE', 'Repsol', true, false, 14.10, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Luis M.', '2026-04-22 08:00:00'),

  ('MOTO', 11, 'XLB481', '2026-04-15 12:00:00',
   13520, NULL, 12760, NULL, 760, NULL,
   1.7, 'GALLONS', 6.435, 15700, 26690, NULL, false,
   'GASOLINA_CORRIENTE', 'Terpel', true, false, NULL, NULL,
   'PENDING_REVIEW', 'Ana G.', '2026-04-15 12:00:00'),

  -- ══════════════ MAYO 2026 ══════════════

  ('MACHINE', v_maq1, 'MAQ-001', '2026-05-08 08:30:00',
   NULL, 4080, NULL, 3950, NULL, 130,
   57.0, 'GALLONS', 215.768, 12500, 712500, NULL, false,
   'ACPM', 'Terpel', true, false, 3.80, 'GALLON_PER_HOUR',
   'PENDING_REVIEW', 'Carlos R.', '2026-05-08 08:30:00'),

  ('MACHINE', v_maq2, 'MAQ-002', '2026-05-10 09:00:00',
   NULL, 2600, NULL, 2440, NULL, 160,
   68.0, 'GALLONS', 257.436, 12480, 848640, NULL, false,
   'ACPM', 'Biomax', true, false, 3.80, 'GALLON_PER_HOUR',
   'PENDING_REVIEW', 'Luis M.', '2026-05-10 09:00:00'),

  ('VEHICLE', 1, 'OGD742', '2026-05-02 07:15:00',
   49180, NULL, 47780, NULL, 1400, NULL,
   12.5, 'GALLONS', 47.318, 16200, 202500, NULL, false,
   'GASOLINA_EXTRA', 'Repsol', true, false, 13.70, 'KM_PER_GALLON',
   'APPROVED', 'Pedro T.', '2026-05-02 07:15:00'),

  ('VEHICLE', 3, 'KBH055', '2026-05-11 07:45:00',
   34470, NULL, 32870, NULL, 1600, NULL,
   14.5, 'GALLONS', 54.888, 16100, 233450, NULL, false,
   'GASOLINA_CORRIENTE', 'Primax', true, false, 14.00, 'KM_PER_GALLON',
   'APPROVED', 'Ana G.', '2026-05-11 07:45:00'),

  ('VEHICLE', 7, 'RTA628', '2026-05-20 08:15:00',
   65520, NULL, 64120, NULL, 1400, NULL,
   11.5, 'GALLONS', 43.532, 16050, 184575, NULL, false,
   'GASOLINA_CORRIENTE', 'Mobil', true, false, 13.80, 'KM_PER_GALLON',
   'PENDING_REVIEW', 'Ana G.', '2026-05-20 08:15:00'),

  ('MOTO', 12, 'CFR076', '2026-05-16 12:00:00',
   11100, NULL, 9700, NULL, 1400, NULL,
   2.0, 'GALLONS', 7.571, 15800, 31600, NULL, false,
   'GASOLINA_CORRIENTE', 'Terpel', true, false, NULL, NULL,
   'APPROVED', 'Pedro T.', '2026-05-16 12:00:00'),

  ('MOTO', 13, 'PPZ340', '2026-05-22 12:00:00',
   23580, NULL, 22180, NULL, 1400, NULL,
   2.2, 'GALLONS', 8.328, 15800, 34760, NULL, false,
   'GASOLINA_CORRIENTE', 'Biomax', true, false, NULL, NULL,
   'PENDING_REVIEW', 'Carlos R.', '2026-05-22 12:00:00');

  RAISE NOTICE 'Seed completado: registros insertados en fuel_logs.';
END;
$$;
