-- ============================================
-- V7: Tabla de Configuración de Cambio de Aceite
-- Basada en rangos reales de km/horas según tipo de aceite y activo
-- ============================================

CREATE TABLE IF NOT EXISTS oil_change_requirements (
    id SERIAL PRIMARY KEY,
    oil_type VARCHAR(50) NOT NULL,          -- MINERAL, SEMI_SYNTHETIC, SYNTHETIC
    asset_type VARCHAR(50) NOT NULL,        -- VEHICLE, MOTORCYCLE, MACHINERY
    km_range INTEGER,                       -- Para vehículos/motos (en km)
    hour_range INTEGER,                     -- Para maquinaria (en horas)
    description VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(oil_type, asset_type, km_range, hour_range)
);

-- ============ MOTOCICLETAS ============
INSERT INTO oil_change_requirements (oil_type, asset_type, km_range, hour_range, description, active) VALUES
('MINERAL', 'MOTORCYCLE', 1500, NULL, 'Mineral - Motocicleta - 1500km', true),
('MINERAL', 'MOTORCYCLE', 2000, NULL, 'Mineral - Motocicleta - 2000km', true),
('SEMI_SYNTHETIC', 'MOTORCYCLE', 3000, NULL, 'Semicintético - Motocicleta - 3000km', true),
('SEMI_SYNTHETIC', 'MOTORCYCLE', 5000, NULL, 'Semicintético - Motocicleta - 5000km', true),
('SYNTHETIC', 'MOTORCYCLE', 6000, NULL, '100% Sintético - Motocicleta - 6000km', true),
('SYNTHETIC', 'MOTORCYCLE', 10000, NULL, '100% Sintético - Motocicleta - 10000km', true);

-- ============ VEHÍCULOS (AUTOS/CARROS) ============
INSERT INTO oil_change_requirements (oil_type, asset_type, km_range, hour_range, description, active) VALUES
('MINERAL', 'VEHICLE', 5000, NULL, 'Mineral - Vehículo - 5000km', true),
('SEMI_SYNTHETIC', 'VEHICLE', 7500, NULL, 'Semicintético - Vehículo - 7500km', true),
('SEMI_SYNTHETIC', 'VEHICLE', 10000, NULL, 'Semicintético - Vehículo - 10000km', true),
('SYNTHETIC', 'VEHICLE', 10000, NULL, '100% Sintético - Vehículo - 10000km', true),
('SYNTHETIC', 'VEHICLE', 15000, NULL, '100% Sintético - Vehículo - 15000km', true);

-- ============ MAQUINARIA (por HOROMETRO) ============
INSERT INTO oil_change_requirements (oil_type, asset_type, km_range, hour_range, description, active) VALUES
('MINERAL', 'MACHINERY', NULL, 250, 'Mineral - Maquinaria - 250 horas', true),
('MINERAL', 'MACHINERY', NULL, 500, 'Mineral - Maquinaria - 500 horas', true),
('SEMI_SYNTHETIC', 'MACHINERY', NULL, 500, 'Semicintético - Maquinaria - 500 horas', true),
('SEMI_SYNTHETIC', 'MACHINERY', NULL, 750, 'Semicintético - Maquinaria - 750 horas', true),
('SYNTHETIC', 'MACHINERY', NULL, 750, '100% Sintético - Maquinaria - 750 horas', true),
('SYNTHETIC', 'MACHINERY', NULL, 1000, '100% Sintético - Maquinaria - 1000 horas', true);

-- Agregar campos a vehicle_oil_changes para vincular con requirements
ALTER TABLE vehicle_oil_changes ADD COLUMN IF NOT EXISTS oil_type VARCHAR(50);
ALTER TABLE vehicle_oil_changes ADD COLUMN IF NOT EXISTS id_requirement INTEGER REFERENCES oil_change_requirements(id);
ALTER TABLE vehicle_oil_changes ADD COLUMN IF NOT EXISTS percentage_used INTEGER;

-- Agregar campos a oil_changes (maquinaria) para vincular con requirements
ALTER TABLE oil_changes ADD COLUMN IF NOT EXISTS oil_type VARCHAR(50);
ALTER TABLE oil_changes ADD COLUMN IF NOT EXISTS id_requirement INTEGER REFERENCES oil_change_requirements(id);
ALTER TABLE oil_changes ADD COLUMN IF NOT EXISTS percentage_used INTEGER;

-- Agregar tabla para análisis SOS (aceite usado) en maquinaria
CREATE TABLE IF NOT EXISTS oil_analysis_sos (
    id SERIAL PRIMARY KEY,
    id_machine INTEGER NOT NULL REFERENCES machines(id),
    machine_name VARCHAR(255) NOT NULL,
    analysis_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    oil_type VARCHAR(50),
    hours_at_analysis INTEGER,
    next_change_hours INTEGER,
    sos_report_url VARCHAR(255),
    approved_by_mechanic VARCHAR(255),
    observations TEXT,
    is_approved BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para optimizar búsquedas
CREATE INDEX IF NOT EXISTS idx_oil_req_oil_asset ON oil_change_requirements(oil_type, asset_type);
CREATE INDEX IF NOT EXISTS idx_vehicle_oil_requirement ON vehicle_oil_changes(id_requirement);
CREATE INDEX IF NOT EXISTS idx_machinery_oil_requirement ON oil_changes(id_requirement);
CREATE INDEX IF NOT EXISTS idx_sos_machine ON oil_analysis_sos(id_machine);
