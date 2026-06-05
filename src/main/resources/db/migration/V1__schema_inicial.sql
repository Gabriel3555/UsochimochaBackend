-- ============================================================
-- V1 — Schema inicial completo
-- Usochicamocha Fleet Management System
-- Aplicar en BD nueva. En BD existente Flyway hace baseline
-- automáticamente (baseline-on-migrate=true, baseline-version=1).
-- ============================================================

-- ── Catálogos (sin dependencias) ────────────────────────────

CREATE TABLE cat_marcas_modelos (
    id_marca    SERIAL PRIMARY KEY,
    descripcion VARCHAR(255)
);

CREATE TABLE cat_tipos_vehiculo (
    id_tipo_vehiculo SERIAL PRIMARY KEY,
    nombre_tipo      VARCHAR(255),
    activo           BOOLEAN
);

CREATE TABLE cat_ubicaciones (
    id_ubicacion     SERIAL PRIMARY KEY,
    nombre_ubicacion VARCHAR(255),
    activo           BOOLEAN
);

CREATE TABLE cat_areas (
    id_area     SERIAL PRIMARY KEY,
    nombre_area VARCHAR(255) UNIQUE,
    activo      BOOLEAN
);

-- ── Usuarios ────────────────────────────────────────────────

CREATE TABLE users (
    id                   BIGSERIAL PRIMARY KEY,
    full_name            VARCHAR(255),
    status               BOOLEAN DEFAULT TRUE,
    username             VARCHAR(255) UNIQUE,
    email                VARCHAR(255),
    role                 VARCHAR(255),
    password             VARCHAR(255),
    license_category     VARCHAR(50),
    license_expiry       DATE,
    license_document_url VARCHAR(1024)
);

-- ── Vehículos ───────────────────────────────────────────────

CREATE TABLE vehiculos (
    id_vehiculo                      SERIAL PRIMARY KEY,
    placa                            VARCHAR(255),
    id_marca                         INTEGER,
    id_tipo_vehiculo                 INTEGER,
    activo                           BOOLEAN,
    kilometraje_actual               INTEGER,
    fecha_ultimo_reporte             TIMESTAMP,
    belongs_to                       VARCHAR(255),
    id_ubicacion_base                INTEGER,
    fuel_tank_capacity_gallons       NUMERIC(8,3),
    factory_efficiency_km_per_gallon NUMERIC(8,2),
    factory_efficiency_unit          VARCHAR(30)
);

-- ── Máquinas ────────────────────────────────────────────────

CREATE TABLE machines (
    id                              BIGSERIAL PRIMARY KEY,
    name                            VARCHAR(255),
    model                           VARCHAR(255),
    belongs_to                      VARCHAR(255),
    soat                            DATE,
    brand                           VARCHAR(255),
    runt                            DATE,
    status                          BOOLEAN,
    num_engine                      VARCHAR(255),
    num_inter_identification        VARCHAR(255),
    fuel_tank_capacity_gallons      NUMERIC(8,3),
    factory_efficiency_gal_per_hour NUMERIC(8,2),
    factory_efficiency_unit         VARCHAR(30)
);

-- ── Marcas de aceite / insumos ──────────────────────────────

CREATE TABLE brands (
    id     BIGSERIAL PRIMARY KEY,
    type   VARCHAR(255),
    name   VARCHAR(255),
    status BOOLEAN DEFAULT TRUE
);

-- ── Repuestos ───────────────────────────────────────────────

CREATE TABLE spare_parts (
    id       BIGSERIAL PRIMARY KEY,
    ref      VARCHAR(255),
    name     VARCHAR(255),
    quantity VARCHAR(255),
    price    NUMERIC(19,2),
    supplier VARCHAR(200)
);

-- ── Mano de obra ────────────────────────────────────────────

CREATE TABLE labor_force (
    id           BIGSERIAL PRIMARY KEY,
    date         TIMESTAMP,
    price        NUMERIC(19,2),
    same_mecanic BOOLEAN,
    mecanic_id   BIGINT,
    contractor   VARCHAR(255),
    observations VARCHAR(255)
);

-- ── Resultados de órdenes ───────────────────────────────────

CREATE TABLE results (
    id             BIGSERIAL PRIMARY KEY,
    date           TIMESTAMP,
    description    VARCHAR(255),
    time_spent     VARCHAR(255),
    hours_spent    INTEGER,
    minutes_spent  INTEGER,
    labor_force_id BIGINT,
    spare_part_id  BIGINT
);

-- ── Inspecciones de maquinaria ──────────────────────────────

CREATE TABLE inspections (
    id                              BIGSERIAL PRIMARY KEY,
    uuid                            VARCHAR(255),
    is_unexpected                   BOOLEAN,
    date_stamp                      TIMESTAMP NOT NULL,
    hour_meter                      DOUBLE PRECISION,
    leak_status                     VARCHAR(255),
    brake_status                    VARCHAR(255),
    belts_pulleys_status            VARCHAR(255),
    tire_lanes_status               VARCHAR(255),
    car_ignition_status             VARCHAR(255),
    electrical_status               VARCHAR(255),
    mechanical_status               VARCHAR(255),
    temperature_status              VARCHAR(255),
    oil_status                      VARCHAR(255),
    hydraulic_status                VARCHAR(255),
    coolant_status                  VARCHAR(255),
    structural_status               VARCHAR(255),
    expiration_date_fire_extinguisher VARCHAR(255),
    observations                    VARCHAR(255),
    greasing_action                 VARCHAR(255),
    greasing_observations           VARCHAR(255),
    machine_id                      BIGINT,
    user_id                         BIGINT
);

CREATE TABLE images (
    id            BIGSERIAL PRIMARY KEY,
    url           VARCHAR(255),
    inspection_id BIGINT
);

-- ── Órdenes de trabajo ──────────────────────────────────────

CREATE TABLE order_counters (
    module_code VARCHAR(5) PRIMARY KEY,
    last_value  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE orders (
    id                    BIGSERIAL PRIMARY KEY,
    status                VARCHAR(255),
    date                  TIMESTAMP,
    description           VARCHAR(255),
    inspection_id         BIGINT,
    vehicle_inspection_id BIGINT,
    result_id             BIGINT,
    assigner_user_id      BIGINT,
    order_type            VARCHAR(50),
    maintenance_type      VARCHAR(30),
    consecutive           VARCHAR(20) UNIQUE
);

-- ── Auditoría ───────────────────────────────────────────────

CREATE TABLE actions_auditory (
    id          BIGSERIAL PRIMARY KEY,
    details     VARCHAR(255),
    perfomer_id BIGINT
);

-- ── Documentación de vehículos ──────────────────────────────

CREATE TABLE documentacion_y_elementos (
    id_documento   SERIAL PRIMARY KEY,
    id_vehiculo    INTEGER,
    tipo_documento VARCHAR(50),
    fecha_vencimiento DATE,       -- nullable: TARJETA DE PROPIEDAD no tiene fecha
    imagen_url     VARCHAR(1024),
    fecha_registro TIMESTAMP,
    registrado_por VARCHAR(100),
    content_type   VARCHAR(120),
    estadodatos    VARCHAR(255),
    fecha_extintor DATE,
    activo         BOOLEAN,
    mesyear        DATE
);

-- ── Inspección pre-operativa de vehículos ───────────────────

CREATE TABLE inspeccion_pre_operativa (
    id_inspeccion        BIGSERIAL PRIMARY KEY,
    fecha_registro       TIMESTAMP,
    id_vehiculo          INTEGER,
    login_user           VARCHAR(100) NOT NULL,
    kilometraje_reportado INTEGER NOT NULL,
    aprobado_ruta        BOOLEAN,
    observaciones_finales VARCHAR(255),
    estado_vehiculo      VARCHAR(255),
    id_ubicacion         INTEGER
);

CREATE TABLE insp_detalle_mecanico (
    id_mecanico       BIGSERIAL PRIMARY KEY,
    id_inspeccion     BIGINT,
    nivel_aceite      VARCHAR(20),
    nivel_refrigerante VARCHAR(20),
    nivel_frenos      VARCHAR(20),
    estado_llantas    VARCHAR(20),
    luces_general     VARCHAR(20),
    estado_visual     VARCHAR(20),
    limpieza_general  VARCHAR(20)
);

CREATE TABLE insp_detalle_documentos (
    id_doc_check  BIGSERIAL PRIMARY KEY,
    id_inspeccion BIGINT,
    check_soat    VARCHAR(20),
    check_tecno   VARCHAR(20),
    check_licencia VARCHAR(20),
    check_extintor VARCHAR(20)
);

CREATE TABLE insp_detalle_elementos (
    id_elementos         BIGSERIAL PRIMARY KEY,
    id_inspeccion        BIGINT,
    tiene_botiquin       BOOLEAN,
    tiene_senalizacion   BOOLEAN,
    tiene_extintor       BOOLEAN,
    tiene_llanta_repuesto VARCHAR(50),
    tiene_gato_hidraulico VARCHAR(50)
);

CREATE TABLE insp_detalle_salud (
    id_salud                  BIGSERIAL PRIMARY KEY,
    id_inspeccion             BIGINT,
    salud_fisica              BOOLEAN,
    salud_mental              BOOLEAN,
    sobrio                    BOOLEAN,
    medicamentos              BOOLEAN,
    condicion_para_conducir   BOOLEAN,
    consciente_responsabilidad BOOLEAN
);

-- ── Mantenimientos ──────────────────────────────────────────

CREATE TABLE mantenimientos (
    id                      BIGSERIAL PRIMARY KEY,
    fecha                   TIMESTAMP NOT NULL,
    id_vehiculo             INTEGER,
    id_ubicacion            INTEGER,
    responsable_asignado    VARCHAR(255),
    kilometraje             INTEGER,
    tipo_mantenimiento      VARCHAR(255),
    repuestos_mantenimiento TEXT,
    taller_responsable      VARCHAR(255),
    observaciones           TEXT
);

-- ── Cambios de aceite ───────────────────────────────────────

CREATE TABLE oil_changes (
    id                   BIGSERIAL PRIMARY KEY,
    date_stamp           TIMESTAMP,
    hydraulic_oil        BOOLEAN,
    motor_oil            BOOLEAN,
    brand                BIGINT,
    quantity             DOUBLE PRECISION,
    hour_meter           DOUBLE PRECISION,
    average_hours_change INTEGER,
    machine_id           BIGINT
);

CREATE TABLE vehicle_oil_changes (
    id                BIGSERIAL PRIMARY KEY,
    date_stamp        TIMESTAMP,
    oil_type          VARCHAR(255),
    brand_id          BIGINT,
    quantity          DOUBLE PRECISION,
    km_at_change      INTEGER,
    interval_km       INTEGER,
    vehicle_id        INTEGER,
    air_filter_changed BOOLEAN
);

-- ── Rendimiento / mano de obra ──────────────────────────────

-- (spare_parts y labor_force ya creados arriba)

-- ── Combustible ─────────────────────────────────────────────

CREATE TABLE fuel_stations (
    id     BIGSERIAL PRIMARY KEY,
    name   VARCHAR(150) NOT NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE fuel_asset_config (
    id                  BIGSERIAL PRIMARY KEY,
    asset_type          VARCHAR(10)  NOT NULL,
    asset_id            BIGINT       NOT NULL,
    tank_capacity_liters NUMERIC(8,2),
    default_unit        VARCHAR(10)  DEFAULT 'LITERS',
    monthly_budget_ref  NUMERIC(14,2),
    updated_at          TIMESTAMP    NOT NULL,
    UNIQUE (asset_type, asset_id)
);

CREATE TABLE fuel_logs (
    id                     BIGSERIAL PRIMARY KEY,
    sync_id                VARCHAR(36) UNIQUE,
    asset_type             VARCHAR(10)  NOT NULL,
    asset_id               BIGINT       NOT NULL,
    asset_plate            VARCHAR(20),
    fuel_date_time         TIMESTAMP    NOT NULL,
    odometer_km            NUMERIC(12,2),
    hour_meter             NUMERIC(10,2),
    previous_odometer      NUMERIC(12,2),
    previous_hour_meter    NUMERIC(10,2),
    delta_km               NUMERIC(12,2),
    delta_hours            NUMERIC(10,2),
    quantity               NUMERIC(8,3) NOT NULL,
    quantity_unit          VARCHAR(10)  NOT NULL,
    quantity_liters        NUMERIC(8,3),
    price_per_unit         NUMERIC(10,2) NOT NULL,
    total_cost_calculated  NUMERIC(12,2) NOT NULL,
    total_cost_actual      NUMERIC(12,2),
    total_cost_mismatch    BOOLEAN      NOT NULL DEFAULT FALSE,
    fuel_type              VARCHAR(25)  NOT NULL,
    service_station        VARCHAR(150),
    is_full_tank           BOOLEAN      NOT NULL DEFAULT TRUE,
    discount_amount        NUMERIC(10,2),
    invoice_photo_url      VARCHAR(500),
    invoice_status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING_REVIEW',
    voucher_number         VARCHAR(50),
    notes                  TEXT,
    efficiency_value       NUMERIC(10,4),
    efficiency_unit        VARCHAR(20),
    is_anomaly             BOOLEAN      NOT NULL DEFAULT FALSE,
    registered_by          VARCHAR(100),
    anomaly_dismissed_by   VARCHAR(100),
    anomaly_dismissed_at   TIMESTAMP,
    anomaly_dismiss_reason TEXT,
    created_at             TIMESTAMP    NOT NULL
);

CREATE TABLE fuel_stats_monthly (
    id            BIGSERIAL PRIMARY KEY,
    year_month    VARCHAR(7)  NOT NULL,
    asset_type    VARCHAR(10) NOT NULL,
    fuel_type     VARCHAR(25) NOT NULL,
    total_gallons NUMERIC(12,4),
    total_cost    NUMERIC(15,2),
    total_loads   INTEGER,
    anomaly_count INTEGER,
    updated_at    TIMESTAMP   NOT NULL,
    UNIQUE (year_month, asset_type, fuel_type)
);

CREATE INDEX idx_fuel_stats_year_month_asset ON fuel_stats_monthly (year_month, asset_type);
