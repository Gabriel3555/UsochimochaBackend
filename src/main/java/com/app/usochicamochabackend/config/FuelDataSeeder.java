package com.app.usochicamochabackend.config;

import com.app.usochicamochabackend.fuel.infrastructure.entity.*;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelLogRepository;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class FuelDataSeeder {

    private static final double LITERS_PER_GALLON = 3.785411784;

    @Bean
    @Order(2)
    CommandLineRunner seedFuelData(
            FuelLogRepository fuelLogRepository,
            MachineRepository machineRepository) {
        return args -> {
            if (fuelLogRepository.count() >= 30) {
                log.info("FuelDataSeeder: ya existen registros suficientes, omitiendo.");
                return;
            }

            // ── 1. Crear máquinas de prueba si no existen ─────────────────────
            List<Long> machineIds = createMachinesIfAbsent(machineRepository);

            // ── 2. Vehículos existentes en el DB (IDs 1-7 = VEHICLE, 8-10 = MOTO)
            record Asset(AssetType type, Long id, String plate) {}

            List<Asset> vehicles = List.of(
                new Asset(AssetType.VEHICLE, 1L,  "OGD742"),
                new Asset(AssetType.VEHICLE, 2L,  "MIF301"),
                new Asset(AssetType.VEHICLE, 3L,  "KBH055"),
                new Asset(AssetType.VEHICLE, 4L,  "NCP187"),
                new Asset(AssetType.VEHICLE, 5L,  "QHE412"),
                new Asset(AssetType.VEHICLE, 6L,  "VLK993"),
                new Asset(AssetType.VEHICLE, 7L,  "RTA628")
            );

            List<Asset> motos = List.of(
                new Asset(AssetType.MOTO, 11L, "XLB481"),
                new Asset(AssetType.MOTO, 12L, "CFR076"),
                new Asset(AssetType.MOTO, 13L, "PPZ340")
            );

            List<Asset> machines = machineIds.stream()
                .map(id -> new Asset(AssetType.MACHINE, id, "MAQ-" + String.format("%03d", id)))
                .toList();

            // ── 3. Generar cargas para 6 meses ────────────────────────────────
            List<FuelLogEntity> logs = new ArrayList<>();

            // Dic 2025 → May 2026
            int[][] months = {{2025, 12}, {2026, 1}, {2026, 2}, {2026, 3}, {2026, 4}, {2026, 5}};

            // Odómetros y horómetros acumulados por activo
            double[] vehOdo   = {45000, 72000, 31500, 98000, 55000, 18000, 63000};
            double[] motoOdo  = {12000, 8500, 21000};
            double[] machHour = {3200, 1800, 4500};

            String[] estaciones = {"Terpel", "Biomax", "Primax", "Mobil", "Texaco", "Repsol"};
            String[] operarios  = {"Carlos R.", "Luis M.", "Ana G.", "Pedro T.", "admin"};

            for (int[] ym : months) {
                int year = ym[0];
                int month = ym[1];

                // ── Máquinas: 2 cargas por máquina por mes ──────────────────
                for (int mi = 0; mi < machines.size(); mi++) {
                    Asset m = machines.get(mi);

                    // 1.ª carga: mediados de mes, lleno completo
                    double gallons1 = 45 + (mi * 8) + (month % 3) * 5;
                    double liters1  = gallons1 * LITERS_PER_GALLON;
                    double price1   = 12200 + (mi * 150) + (month % 2) * 100; // COP/gal ACPM
                    double cost1    = gallons1 * price1;
                    machHour[mi] += 130 + mi * 20;

                    logs.add(buildLog(
                        m.type(), m.id(), m.plate(),
                        LocalDateTime.of(year, month, 10, 8, 30),
                        null, round(machHour[mi]),
                        null, round(machHour[mi] - 130 - mi * 20),
                        gallons1, price1, cost1, null,
                        FuelType.ACPM, estaciones[mi % estaciones.length],
                        true, false, operarios[mi % operarios.length]
                    ));

                    // 2.ª carga: fin de mes
                    double gallons2 = 35 + (mi * 5) + (month % 4) * 3;
                    double liters2  = gallons2 * LITERS_PER_GALLON;
                    double price2   = 12100 + (mi * 100);
                    double cost2    = gallons2 * price2;
                    double prevHour = machHour[mi];
                    machHour[mi] += 100 + mi * 15;

                    logs.add(buildLog(
                        m.type(), m.id(), m.plate(),
                        LocalDateTime.of(year, month, 25, 14, 0),
                        null, round(machHour[mi]),
                        null, round(prevHour),
                        gallons2, price2, cost2, null,
                        FuelType.ACPM, estaciones[(mi + 2) % estaciones.length],
                        false, false, operarios[(mi + 1) % operarios.length]
                    ));
                }

                // ── Vehículos: 1 carga por vehículo por mes (alternando) ─────
                for (int vi = 0; vi < vehicles.size(); vi++) {
                    Asset v = vehicles.get(vi);
                    double gallons = 10 + (vi % 3) * 2 + (month % 3);
                    double price   = 15800 + (vi * 80) + (month % 3) * 50; // COP/gal gasolina
                    double cost    = gallons * price;
                    double prevOdo = vehOdo[vi];
                    vehOdo[vi] += 650 + vi * 30 + (month % 4) * 40;

                    // Introducir discrepancia de costo en 2 registros
                    Double costActual = (vi == 2 && month == 2) ? cost * 0.97 : null;

                    FuelType ft = (vi % 3 == 0) ? FuelType.GASOLINA_EXTRA : FuelType.GASOLINA_CORRIENTE;

                    logs.add(buildLog(
                        v.type(), v.id(), v.plate(),
                        LocalDateTime.of(year, month, 5 + vi * 3, 7 + vi, 15),
                        round(vehOdo[vi]), null,
                        round(prevOdo), null,
                        gallons, price, cost, costActual,
                        ft, estaciones[(vi + month) % estaciones.length],
                        true, false, operarios[vi % operarios.length]
                    ));
                }

                // ── Motos: 1 carga por moto por mes ─────────────────────────
                for (int oi = 0; oi < motos.size(); oi++) {
                    Asset mo = motos.get(oi);
                    double gallons = 1.5 + (oi * 0.4) + (month % 3) * 0.2;
                    double price   = 15600 + (oi * 60);
                    double cost    = gallons * price;
                    double prevOdo = motoOdo[oi];
                    motoOdo[oi] += 380 + oi * 20;

                    logs.add(buildLog(
                        mo.type(), mo.id(), mo.plate(),
                        LocalDateTime.of(year, month, 15 + oi * 3, 12, 0),
                        round(motoOdo[oi]), null,
                        round(prevOdo), null,
                        gallons, price, cost, null,
                        FuelType.GASOLINA_CORRIENTE,
                        estaciones[oi % estaciones.length],
                        true, false, operarios[(oi + 2) % operarios.length]
                    ));
                }
            }

            // ── 4. Inyectar anomalías ─────────────────────────────────────────
            // Marcar 3 registros de vehículo como anómalos (consumo extremo)
            long anomalyCount = 0;
            for (FuelLogEntity e : logs) {
                if (e.getAssetType() == AssetType.VEHICLE && anomalyCount < 3) {
                    e.setIsAnomaly(true);
                    e.setEfficiencyValue(new BigDecimal("4.20")); // muy bajo: 4.2 km/Gal
                    e.setEfficiencyUnit("KM_PER_GALLON");
                    anomalyCount++;
                }
            }
            // Registros normales de eficiencia
            long effCount = 0;
            for (FuelLogEntity e : logs) {
                if (!Boolean.TRUE.equals(e.getIsAnomaly()) && e.getIsFullTank()
                        && e.getAssetType() == AssetType.VEHICLE && effCount < 6) {
                    e.setEfficiencyValue(new BigDecimal("12.50").add(BigDecimal.valueOf(effCount)));
                    e.setEfficiencyUnit("KM_PER_GALLON");
                    effCount++;
                }
                if (!Boolean.TRUE.equals(e.getIsAnomaly()) && e.getIsFullTank()
                        && e.getAssetType() == AssetType.MACHINE) {
                    e.setEfficiencyValue(new BigDecimal("3.80"));
                    e.setEfficiencyUnit("GALLON_PER_HOUR");
                }
            }

            fuelLogRepository.saveAll(logs);
            log.info("FuelDataSeeder: {} registros de combustible creados.", logs.size());
        };
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<Long> createMachinesIfAbsent(MachineRepository repo) {
        if (repo.count() >= 3) {
            return repo.findAll().stream().limit(3).map(MachineEntity::getId).toList();
        }
        String[][] data = {
            {"Excavadora hidráulica", "CAT 320D", "Caterpillar"},
            {"Buldócer",             "D6N XL",   "Caterpillar"},
            {"Retroexcavadora",      "3CX PRO",   "JCB"}
        };
        List<Long> ids = new ArrayList<>();
        for (String[] d : data) {
            MachineEntity m = MachineEntity.builder()
                .name(d[0]).model(d[1]).brand(d[2])
                .status(true)
                .belongsTo("Usochicamocha")
                .numInterIdentification("USO-" + d[1].replace(" ", ""))
                .build();
            ids.add(repo.save(m).getId());
        }
        log.info("FuelDataSeeder: {} máquinas de prueba creadas.", ids.size());
        return ids;
    }

    private FuelLogEntity buildLog(
            AssetType type, Long assetId, String plate,
            LocalDateTime fuelDate,
            BigDecimal odometer, BigDecimal hourMeter,
            BigDecimal prevOdo, BigDecimal prevHour,
            double gallons, double pricePerGallon, double calculatedCost, Double actualCost,
            FuelType fuelType, String station,
            boolean fullTank, boolean anomaly, String registeredBy) {

        BigDecimal qty        = bd(gallons, 3);
        BigDecimal liters     = bd(gallons * LITERS_PER_GALLON, 3);
        BigDecimal price      = bd(pricePerGallon, 2);
        BigDecimal calcCost   = bd(calculatedCost, 2);
        boolean mismatch      = actualCost != null
                && Math.abs(calculatedCost - actualCost) / calculatedCost > 0.01;

        BigDecimal deltaKm = null;
        if (odometer != null && prevOdo != null) {
            BigDecimal d = odometer.subtract(prevOdo);
            if (d.compareTo(BigDecimal.ZERO) > 0) deltaKm = d;
        }
        BigDecimal deltaH = null;
        if (hourMeter != null && prevHour != null) {
            BigDecimal d = hourMeter.subtract(prevHour);
            if (d.compareTo(BigDecimal.ZERO) > 0) deltaH = d;
        }

        return FuelLogEntity.builder()
            .assetType(type)
            .assetId(assetId)
            .assetPlate(plate)
            .fuelDateTime(fuelDate)
            .odometerKm(odometer)
            .hourMeter(hourMeter)
            .previousOdometer(prevOdo)
            .previousHourMeter(prevHour)
            .deltaKm(deltaKm)
            .deltaHours(deltaH)
            .quantity(qty)
            .quantityUnit(QuantityUnit.GALLONS)
            .quantityLiters(liters)
            .pricePerUnit(price)
            .totalCostCalculated(calcCost)
            .totalCostActual(actualCost != null ? bd(actualCost, 2) : null)
            .totalCostMismatch(mismatch)
            .fuelType(fuelType)
            .serviceStation(station)
            .isFullTank(fullTank)
            .isAnomaly(anomaly)
            .invoiceStatus(InvoiceStatus.PENDING_REVIEW)
            .registeredBy(registeredBy)
            .createdAt(fuelDate)
            .build();
    }

    private static BigDecimal bd(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
