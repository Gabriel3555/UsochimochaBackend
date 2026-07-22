-- ============================================
-- V21: Deduplicar cat_tipos_vehiculo y evitar que vuelva a duplicarse
--
-- cat_tipos_vehiculo.nombre_tipo nunca tuvo un constraint UNIQUE. Ya existía
-- la fila id=3 ('Motocicleta', mixúscula, usada por los 13 vehículos reales)
-- cuando V5__cleanup_and_insert_tipo_motocicleta.sql corrió su chequeo de
-- existencia con comparación EXACTA y sensible a mayúsculas
-- (WHERE nombre_tipo = 'MOTOCICLETA'); no encontró esa fila como "ya
-- existente" y insertó una segunda (id=10, 'MOTOCICLETA' todo mayúsculas,
-- huérfana, 0 vehículos). Confirmado en prod 2026-07-22.
-- MotoService.createMoto/updateMoto buscan con
-- findByNombreTipoIgnoreCase("MOTOCICLETA"), que devuelve Optional<T> (espera
-- un único resultado); con las 2 filas, Spring Data lanzaba
-- IncorrectResultSizeDataAccessException ("Query did not return a unique
-- result: 2 results were returned") en cada edición/creación de moto,
-- revirtiendo toda la transacción (incluido el kilometraje).
--
-- Esta migración:
--   1) Para cada grupo de nombre_tipo duplicado (case-insensitive), conserva
--      la fila de menor id como canónica.
--   2) Reapunta cualquier vehículo que referencie una fila "perdedora" hacia
--      la canónica.
--   3) Borra las filas duplicadas.
--   4) Crea un índice único case-insensitive para que no vuelva a ocurrir.
-- ============================================

-- NULLs se excluyen a propósito: nunca causaron la ambigüedad (las búsquedas
-- siempre son por un nombre concreto, no por NULL) y DISTINCT ON agruparía
-- todas las filas NULL entre sí, fusionando catálogos sin relación.

WITH canonical AS (
    SELECT DISTINCT ON (UPPER(nombre_tipo))
        id_tipo_vehiculo AS canonical_id,
        UPPER(nombre_tipo) AS nombre_norm
    FROM cat_tipos_vehiculo
    WHERE nombre_tipo IS NOT NULL
    ORDER BY UPPER(nombre_tipo), id_tipo_vehiculo
),
losers AS (
    SELECT t.id_tipo_vehiculo AS loser_id, c.canonical_id
    FROM cat_tipos_vehiculo t
    JOIN canonical c ON UPPER(t.nombre_tipo) = c.nombre_norm
    WHERE t.id_tipo_vehiculo <> c.canonical_id
)
UPDATE vehiculos v
SET id_tipo_vehiculo = l.canonical_id
FROM losers l
WHERE v.id_tipo_vehiculo = l.loser_id;

WITH canonical AS (
    SELECT DISTINCT ON (UPPER(nombre_tipo))
        id_tipo_vehiculo AS canonical_id,
        UPPER(nombre_tipo) AS nombre_norm
    FROM cat_tipos_vehiculo
    WHERE nombre_tipo IS NOT NULL
    ORDER BY UPPER(nombre_tipo), id_tipo_vehiculo
)
DELETE FROM cat_tipos_vehiculo t
USING canonical c
WHERE UPPER(t.nombre_tipo) = c.nombre_norm
  AND t.id_tipo_vehiculo <> c.canonical_id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_cat_tipos_vehiculo_nombre_upper
    ON cat_tipos_vehiculo (UPPER(nombre_tipo))
    WHERE nombre_tipo IS NOT NULL;
