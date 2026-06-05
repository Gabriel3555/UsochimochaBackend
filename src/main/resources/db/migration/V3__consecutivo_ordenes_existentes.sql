-- ============================================================
-- V3 — Renumerar TODAS las órdenes desde 1 en orden cronológico
-- Tres prefijos:
--   OT-MQ-XXXXX  → maquinaria  (inspection_id IS NOT NULL)
--   OT-VH-XXXXX  → vehículos   (vehicle_inspection_id + tipo NO es moto)
--   OT-MT-XXXXX  → motocicletas(vehicle_inspection_id + tipo SÍ es moto)
-- ============================================================

DO $$
DECLARE
    mq_counter INTEGER := 0;
    vh_counter INTEGER := 0;
    mt_counter INTEGER := 0;
    r           RECORD;
BEGIN
    -- Limpiar todo para renumerar desde 1
    UPDATE orders SET consecutive = NULL;
    DELETE FROM order_counters WHERE module_code IN ('MQ', 'VH', 'MT');

    -- ── Maquinaria ───────────────────────────────────────────
    FOR r IN
        SELECT id FROM orders
        WHERE inspection_id IS NOT NULL
        ORDER BY id
    LOOP
        mq_counter := mq_counter + 1;
        UPDATE orders
            SET consecutive = 'OT-MQ-' || LPAD(mq_counter::TEXT, 5, '0')
        WHERE id = r.id;
    END LOOP;

    -- ── Vehículos (excluye motos) ────────────────────────────
    FOR r IN
        SELECT o.id
        FROM orders o
        JOIN inspeccion_pre_operativa i ON o.vehicle_inspection_id = i.id_inspeccion
        JOIN vehiculos                v ON i.id_vehiculo            = v.id_vehiculo
        JOIN cat_tipos_vehiculo       t ON v.id_tipo_vehiculo       = t.id_tipo_vehiculo
        WHERE o.vehicle_inspection_id IS NOT NULL
          AND UPPER(t.nombre_tipo) NOT LIKE '%MOTO%'
        ORDER BY o.id
    LOOP
        vh_counter := vh_counter + 1;
        UPDATE orders
            SET consecutive = 'OT-VH-' || LPAD(vh_counter::TEXT, 5, '0')
        WHERE id = r.id;
    END LOOP;

    -- ── Motocicletas ─────────────────────────────────────────
    FOR r IN
        SELECT o.id
        FROM orders o
        JOIN inspeccion_pre_operativa i ON o.vehicle_inspection_id = i.id_inspeccion
        JOIN vehiculos                v ON i.id_vehiculo            = v.id_vehiculo
        JOIN cat_tipos_vehiculo       t ON v.id_tipo_vehiculo       = t.id_tipo_vehiculo
        WHERE o.vehicle_inspection_id IS NOT NULL
          AND UPPER(t.nombre_tipo) LIKE '%MOTO%'
        ORDER BY o.id
    LOOP
        mt_counter := mt_counter + 1;
        UPDATE orders
            SET consecutive = 'OT-MT-' || LPAD(mt_counter::TEXT, 5, '0')
        WHERE id = r.id;
    END LOOP;

    -- ── Actualizar contadores ────────────────────────────────
    INSERT INTO order_counters (module_code, last_value) VALUES ('MQ', mq_counter);
    INSERT INTO order_counters (module_code, last_value) VALUES ('VH', vh_counter);
    INSERT INTO order_counters (module_code, last_value) VALUES ('MT', mt_counter);
END$$;
