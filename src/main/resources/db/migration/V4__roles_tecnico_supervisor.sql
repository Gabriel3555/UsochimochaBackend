-- ============================================================
-- V4 — Migración de roles al nuevo esquema de 4 roles
-- ACEITE  → TECNICO  (técnico de aceite/combustible, solo móvil)
-- MECANIC → TECNICO  (rol legacy, mismo acceso que TECNICO)
-- Los roles ADMIN, OPERARIO y el nuevo SUPERVISOR no cambian.
-- Seguro para producción: sin pérdida de datos ni de acceso.
-- ============================================================

UPDATE users
    SET role = 'TECNICO'
WHERE role IN ('ACEITE', 'MECANIC');
