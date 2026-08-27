-- Motivo del reintegro (opcional) — a pedido del usuario, para dejar constancia de
-- por qué se devolvió combustible sobrante de un tanqueo.
ALTER TABLE fuel_reintegrations ADD COLUMN motivo VARCHAR(255);

-- Bug encontrado de paso: FuelReintegrationService ya guardaba valor_reintegro=NULL
-- para reintegros de tanqueos en ALMACEN (no se valorizan, solo BOMBA tiene precio
-- propio), pero la columna quedó NOT NULL desde la V20 — un INSERT real contra
-- Postgres habría fallado (los tests con H2 no lo detectaban, H2 no aplica NOT NULL
-- igual que no aplicaba el CHECK que corrigió la V23). Se relaja para que coincida
-- con el comportamiento real del servicio.
ALTER TABLE fuel_reintegrations ALTER COLUMN valor_reintegro DROP NOT NULL;
