-- fuel_types: catálogo, patrón consistente con el dominio catalog/
CREATE TABLE fuel_types (
  id            BIGSERIAL PRIMARY KEY,
  codigo        VARCHAR(30)  NOT NULL UNIQUE,
  nombre        VARCHAR(60)  NOT NULL,
  activo        BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO fuel_types (codigo, nombre) VALUES
  ('GASOLINA_CORRIENTE', 'Gasolina corriente'),
  ('GASOLINA_EXTRA',     'Gasolina extra'),
  ('ACPM',               'ACPM / Diésel'),
  ('GAS',                'Gas natural vehicular');

-- asset_fuel_config: rendimiento estándar por activo (arco exclusivo) — datos para Fase 3, tabla ya disponible
CREATE TABLE asset_fuel_config (
  id                    BIGSERIAL PRIMARY KEY,
  vehicle_id            INTEGER  REFERENCES vehiculos(id_vehiculo),
  machine_id            BIGINT   REFERENCES machines(id),
  consumo_estandar      NUMERIC(10,4) NOT NULL,
  unidad_consumo        VARCHAR(20)   NOT NULL
                         CHECK (unidad_consumo IN ('GAL_POR_HORA','KM_POR_GALON')),
  fuel_type_default_id  BIGINT NOT NULL REFERENCES fuel_types(id),
      tanque_capacidad_gal  NUMERIC(8,2),
  CHECK (num_nonnulls(vehicle_id, machine_id) = 1)
);

-- fuel_inventory: saldo del almacén propio, por área de costo
CREATE TABLE fuel_inventory (
  id                  BIGSERIAL PRIMARY KEY,
  area_costo          VARCHAR(12) NOT NULL CHECK (area_costo IN ('DISTRITO','ASOCIACION')),
  fuel_type_id        BIGINT NOT NULL REFERENCES fuel_types(id),
  cantidad_disponible NUMERIC(10,3) NOT NULL DEFAULT 0,
  actualizado_en      TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (area_costo, fuel_type_id)
);

INSERT INTO fuel_inventory (area_costo, fuel_type_id, cantidad_disponible)
SELECT ac.area_costo, ft.id, 0
FROM (VALUES ('DISTRITO'), ('ASOCIACION')) AS ac(area_costo)
CROSS JOIN fuel_types ft;

-- fuel_purchases: entrada de stock (compra a proveedor)
CREATE TABLE fuel_purchases (
  id                 BIGSERIAL PRIMARY KEY,
  area_costo         VARCHAR(12) NOT NULL CHECK (area_costo IN ('DISTRITO','ASOCIACION')),
  fuel_type_id       BIGINT NOT NULL REFERENCES fuel_types(id),
  cantidad           NUMERIC(10,3) NOT NULL,
  precio_unitario    NUMERIC(10,2) NOT NULL,
  descuento          NUMERIC(10,2) DEFAULT 0,
  total_ingresado    NUMERIC(12,2) NOT NULL,
  total_calculado    NUMERIC(12,2) NOT NULL,
  discrepancia_valor BOOLEAN NOT NULL DEFAULT FALSE,
  url_factura        VARCHAR(500) NOT NULL,
  responsable_id     BIGINT NOT NULL REFERENCES users(id),
  fecha_compra       TIMESTAMP NOT NULL DEFAULT now(),
  status             BOOLEAN NOT NULL DEFAULT TRUE
);

-- refueling_records: el tanqueo en sí (arco exclusivo vehicle/machine)
CREATE TABLE refueling_records (
  id                 BIGSERIAL PRIMARY KEY,
  vehicle_id         INTEGER REFERENCES vehiculos(id_vehiculo),
  machine_id         BIGINT  REFERENCES machines(id),
  lugar              VARCHAR(10) NOT NULL CHECK (lugar IN ('BOMBA','ALMACEN')),
  area_costo         VARCHAR(12) NOT NULL CHECK (area_costo IN ('DISTRITO','ASOCIACION')),
  fuel_type_id       BIGINT NOT NULL REFERENCES fuel_types(id),
  cantidad_galones   NUMERIC(8,3) NOT NULL,
  horometro_km       NUMERIC(12,2) NOT NULL,
  es_full            BOOLEAN NOT NULL DEFAULT FALSE,
  precio_unitario    NUMERIC(10,2),
  descuento          NUMERIC(10,2),
  total_ingresado    NUMERIC(12,2),
  total_calculado    NUMERIC(12,2),
  discrepancia_valor BOOLEAN NOT NULL DEFAULT FALSE,
  url_factura        VARCHAR(500),
  origen             VARCHAR(150),
  responsable_id     BIGINT NOT NULL REFERENCES users(id),
  fecha_registro     TIMESTAMP NOT NULL DEFAULT now(),
  status             BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK (num_nonnulls(vehicle_id, machine_id) = 1),
  CHECK (lugar <> 'BOMBA' OR (precio_unitario IS NOT NULL AND url_factura IS NOT NULL))
);

-- fuel_reintegrations: devolución de combustible sobrante (Service/Controller en Fase 3)
CREATE TABLE fuel_reintegrations (
  id                   BIGSERIAL PRIMARY KEY,
  refueling_id         BIGINT NOT NULL REFERENCES refueling_records(id),
  cantidad_reintegrada NUMERIC(8,3) NOT NULL,
  valor_reintegro      NUMERIC(12,2) NOT NULL,
  responsable_id       BIGINT NOT NULL REFERENCES users(id),
  fecha_reintegro      TIMESTAMP NOT NULL DEFAULT now()
);
