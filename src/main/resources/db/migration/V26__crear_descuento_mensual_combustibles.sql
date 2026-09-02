-- Descuento mensual global de combustible (a pedido del usuario): el proveedor
-- no informa el descuento por cada tanqueo individual, solo un total
-- consolidado a fin de mes/periodo. En vez de forzar un mes calendario exacto,
-- se registra con un rango libre (fecha_inicio/fecha_fin) para cubrir el
-- periodo real que informe el proveedor.
CREATE TABLE fuel_monthly_discounts (
    id BIGSERIAL PRIMARY KEY,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    monto NUMERIC(12, 2) NOT NULL,
    responsable_id BIGINT,
    fecha_registro TIMESTAMP NOT NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE
);
