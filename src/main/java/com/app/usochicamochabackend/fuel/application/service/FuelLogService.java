package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.exception.BadRequestException;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.fuel.application.dto.FuelDashboardResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelLogRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelLogResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelMonthlyStatsResponse;
import com.app.usochicamochabackend.fuel.application.port.CreateFuelLogUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetFuelDashboardUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetFuelHistoryUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.*;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelLogRepository;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FuelLogService implements CreateFuelLogUseCase, GetFuelHistoryUseCase, GetFuelDashboardUseCase {

    private final FuelLogRepository fuelLogRepository;
    private final MachineRepository machineRepository;
    private final VehicleRepository vehicleRepository;
    private final SaveActionUseCase saveActionUseCase;
    private final NotificationService notificationService;

    @Value("${fuel.max-quantity.machine:500}")
    private double maxQuantityMachine;

    @Value("${fuel.max-quantity.vehicle:120}")
    private double maxQuantityVehicle;

    @Value("${fuel.max-quantity.moto:25}")
    private double maxQuantityMoto;

    // ──────────────────────────────────────────────────────────────────────────
    // CreateFuelLogUseCase
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public FuelLogResponse createFuelLog(FuelLogRequest request) {
        // Idempotency: if mobile sends a syncId we already have, return the existing record
        if (request.syncId() != null && !request.syncId().isBlank()) {
            Optional<FuelLogEntity> existing = fuelLogRepository.findBySyncId(request.syncId());
            if (existing.isPresent()) {
                log.info("Duplicate syncId={} received, returning existing fuel log id={}", request.syncId(), existing.get().getId());
                return toResponse(existing.get());
            }
        }

        AssetType assetType = parseAssetType(request.assetType());
        validateAssetExists(assetType, request.assetId());

        QuantityUnit unit = parseQuantityUnit(request.quantityUnit());
        Double litersDouble = unit.toLiters(request.quantity().doubleValue());
        BigDecimal quantityLiters = litersDouble != null
                ? BigDecimal.valueOf(litersDouble).setScale(3, RoundingMode.HALF_UP)
                : null; // GAS_NATURAL en m³ no tiene equivalente en litros

        if (quantityLiters != null) {
            validateMaxQuantity(assetType, request.assetId(), quantityLiters);
        }

        // Find previous reading for continuity check and delta calculation
        FuelLogEntity previous = fuelLogRepository
                .findFirstByAssetTypeAndAssetIdOrderByFuelDateTimeDesc(assetType, request.assetId())
                .orElse(null);

        validateContinuity(assetType, request, previous);

        BigDecimal previousOdometer = previous != null ? previous.getOdometerKm() : null;
        BigDecimal previousHourMeter = previous != null ? previous.getHourMeter() : null;
        BigDecimal deltaKm = computeDeltaKm(request.odometerKm(), previousOdometer);
        BigDecimal deltaHours = computeDeltaHours(request.hourMeter(), previousHourMeter);

        BigDecimal totalCostCalculated = request.quantity().multiply(request.pricePerUnit())
                .setScale(2, RoundingMode.HALF_UP);

        boolean mismatch = detectCostMismatch(totalCostCalculated, request.totalCostActual());

        String username = resolveUsername();
        LocalDateTime fuelDateTime = request.fuelDateTime() != null ? request.fuelDateTime() : LocalDateTime.now();

        FuelLogEntity entity = FuelLogEntity.builder()
                .syncId(request.syncId())
                .assetType(assetType)
                .assetId(request.assetId())
                .assetPlate(request.assetPlate())
                .fuelDateTime(fuelDateTime)
                .odometerKm(request.odometerKm())
                .hourMeter(request.hourMeter())
                .previousOdometer(previousOdometer)
                .previousHourMeter(previousHourMeter)
                .deltaKm(deltaKm)
                .deltaHours(deltaHours)
                .quantity(request.quantity())
                .quantityUnit(unit)
                .quantityLiters(quantityLiters)
                .pricePerUnit(request.pricePerUnit())
                .totalCostCalculated(totalCostCalculated)
                .totalCostActual(request.totalCostActual())
                .totalCostMismatch(mismatch)
                .fuelType(parseFuelType(request.fuelType()))
                .serviceStation(request.serviceStation())
                .isFullTank(request.isFullTank() != null ? request.isFullTank() : true)
                .discountAmount(request.discountAmount())
                .invoicePhotoUrl(request.invoicePhotoUrl())
                .invoiceStatus(InvoiceStatus.PENDING_REVIEW)
                .voucherNumber(request.voucherNumber())
                .notes(request.notes())
                .isAnomaly(mismatch) // discrepancia de costo ya es una anomalía
                .registeredBy(username)
                .build();

        FuelLogEntity saved = fuelLogRepository.save(entity);

        if (mismatch) {
            notificationService.notifyFuelAnomaly(
                    "Discrepancia de costo detectada: " + assetType.name() +
                    " ID " + request.assetId() +
                    " | calculado: $" + totalCostCalculated +
                    " vs declarado: $" + request.totalCostActual());
            log.warn("Cost mismatch anomaly for {} id={}: calculated={} declared={}",
                    assetType, request.assetId(), totalCostCalculated, request.totalCostActual());
        }

        // Calculate efficiency and anomaly only for full-tank fills (OR con el mismatch ya marcado)
        if (Boolean.TRUE.equals(saved.getIsFullTank())) {
            saved = calculateAndPersistEfficiency(saved);
        }

        saveActionUseCase.save("El usuario " + username + " registró carga de combustible para " +
                assetType.name() + " ID " + request.assetId() +
                " — " + request.quantity() + " " + unit.name() + " de " + request.fuelType());

        notificationService.notifyDataUpdate("fuel-updated");

        return toResponse(saved);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GetFuelHistoryUseCase
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public List<FuelLogResponse> getHistoryByAsset(String assetType, Long assetId) {
        AssetType type = parseAssetType(assetType);
        return fuelLogRepository.findByAssetTypeAndAssetIdOrderByFuelDateTimeDesc(type, assetId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<FuelLogResponse> getHistoryByAssetAndDateRange(String assetType, Long assetId,
                                                                LocalDateTime from, LocalDateTime to) {
        AssetType type = parseAssetType(assetType);
        return fuelLogRepository.findByAssetAndDateRange(type, assetId, from, to)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public FuelLogResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public FuelLogResponse updateInvoiceStatus(Long id, String status) {
        FuelLogEntity entity = findOrThrow(id);
        entity.setInvoiceStatus(InvoiceStatus.valueOf(status.toUpperCase()));
        return toResponse(fuelLogRepository.save(entity));
    }

    @Override
    @Transactional
    public FuelLogResponse dismissAnomaly(Long id, String reason, BigDecimal correctedCost) {
        FuelLogEntity entity = findOrThrow(id);
        String username = resolveUsername();
        entity.setIsAnomaly(false);
        entity.setAnomalyDismissedBy(username);
        entity.setAnomalyDismissedAt(LocalDateTime.now());
        entity.setAnomalyDismissReason(reason);
        if (correctedCost != null) {
            entity.setTotalCostActual(correctedCost);
            entity.setTotalCostMismatch(detectCostMismatch(entity.getTotalCostCalculated(), correctedCost));
        }
        FuelLogEntity saved = fuelLogRepository.save(entity);
        saveActionUseCase.save("El usuario " + username + " descartó anomalía del registro de combustible ID " + id);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public FuelLogResponse uploadInvoiceFile(Long id, MultipartFile file) {
        FuelLogEntity entity = findOrThrow(id);
        try {
            String ext = Optional.ofNullable(file.getOriginalFilename())
                    .filter(n -> n.contains("."))
                    .map(n -> n.substring(n.lastIndexOf('.')))
                    .orElse(".jpg");
            Path dir = Paths.get("uploads", "fuel", "invoices");
            Files.createDirectories(dir);
            String filename = "fuel_" + id + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target);
            entity.setInvoicePhotoUrl("uploads/fuel/invoices/" + filename);
            return toResponse(fuelLogRepository.save(entity));
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la factura: " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GetFuelDashboardUseCase
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public FuelDashboardResponse getDashboard(LocalDateTime from, LocalDateTime to) {
        List<FuelLogEntity> logs = fetchLogs(from, to);

        BigDecimal totalLiters = logs.stream()
                .map(FuelLogEntity::getQuantityLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGallons = litersToGallons(totalLiters);

        BigDecimal totalCost = logs.stream()
                .map(FuelLogEntity::getTotalCostCalculated)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long machineCount = logs.stream().filter(l -> l.getAssetType() == AssetType.MACHINE).count();
        long vehicleCount = logs.stream().filter(l -> l.getAssetType() == AssetType.VEHICLE).count();
        long motoCount = logs.stream().filter(l -> l.getAssetType() == AssetType.MOTO).count();
        long anomalyCount = logs.stream().filter(l -> Boolean.TRUE.equals(l.getIsAnomaly())).count();
        long pendingInvoiceCount = logs.stream()
                .filter(l -> l.getInvoiceStatus() == InvoiceStatus.PENDING_REVIEW).count();

        List<FuelLogResponse> recentLogs = logs.stream()
                .limit(50).map(this::toResponse).collect(Collectors.toList());

        return new FuelDashboardResponse(logs.size(), totalGallons, totalCost,
                machineCount, vehicleCount, motoCount, anomalyCount, pendingInvoiceCount, recentLogs);
    }

    @Override
    public List<FuelLogResponse> getAllLogs(LocalDateTime from, LocalDateTime to) {
        return fetchLogs(from, to).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<FuelLogResponse> getAnomalies() {
        return fuelLogRepository.findByIsAnomalyTrueOrderByFuelDateTimeDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<FuelLogResponse> getRanking(LocalDateTime from, LocalDateTime to) {
        List<FuelLogEntity> logs = fetchLogs(from, to);

        // Group by (assetType, assetId) and keep the record with the most consumption
        Map<String, List<FuelLogEntity>> grouped = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getAssetType().name() + "_" + l.getAssetId()));

        return grouped.values().stream()
                .map(group -> {
                    BigDecimal totalLiters = group.stream()
                            .map(FuelLogEntity::getQuantityLiters)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalGallons = litersToGallons(totalLiters);
                    BigDecimal totalCost = group.stream()
                            .map(FuelLogEntity::getTotalCostCalculated)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    FuelLogEntity rep = group.stream()
                            .max(Comparator.comparing(FuelLogEntity::getFuelDateTime))
                            .orElseThrow();
                    return toRankingResponse(rep, totalGallons, totalCost, group.size());
                })
                // Menor eficiencia primero (km/gal ascendente; nulls al final)
                .sorted(Comparator.comparing(
                        (FuelLogResponse r) -> r.efficiencyValue() != null ? r.efficiencyValue() : BigDecimal.valueOf(Double.MAX_VALUE)))
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private FuelLogEntity calculateAndPersistEfficiency(FuelLogEntity saved) {
        List<FuelLogEntity> topTwo = fuelLogRepository
                .findTop2ByAssetTypeAndAssetIdAndIsFullTankTrueOrderByFuelDateTimeDesc(
                        saved.getAssetType(), saved.getAssetId());

        if (topTwo.size() < 2) {
            return saved;
        }

        FuelLogEntity current = topTwo.get(0);
        FuelLogEntity prevFull = topTwo.get(1);

        if (!current.getId().equals(saved.getId())) {
            return saved;
        }

        BigDecimal accumulatedLiters = fuelLogRepository.sumQuantityLitersBetween(
                saved.getAssetType(), saved.getAssetId(),
                prevFull.getFuelDateTime(), current.getFuelDateTime());

        if (accumulatedLiters == null || accumulatedLiters.compareTo(BigDecimal.ZERO) == 0) {
            return saved;
        }

        BigDecimal accumulatedGallons = accumulatedLiters.divide(
                BigDecimal.valueOf(3.785411784), 4, RoundingMode.HALF_UP);

        BigDecimal efficiencyValue = null;
        String efficiencyUnit = null;

        if (saved.getAssetType() == AssetType.VEHICLE || saved.getAssetType() == AssetType.MOTO) {
            if (current.getOdometerKm() != null && prevFull.getOdometerKm() != null) {
                BigDecimal delta = current.getOdometerKm().subtract(prevFull.getOdometerKm());
                if (delta.compareTo(BigDecimal.ZERO) > 0) {
                    efficiencyValue = delta.divide(accumulatedGallons, 4, RoundingMode.HALF_UP);
                    efficiencyUnit = "KM_PER_GALLON";
                }
            }
        } else {
            if (current.getHourMeter() != null && prevFull.getHourMeter() != null) {
                BigDecimal delta = current.getHourMeter().subtract(prevFull.getHourMeter());
                if (delta.compareTo(BigDecimal.ZERO) > 0) {
                    efficiencyValue = accumulatedGallons.divide(delta, 4, RoundingMode.HALF_UP);
                    efficiencyUnit = "GALLON_PER_HOUR";
                }
            }
        }

        if (efficiencyValue == null) {
            return saved;
        }

        boolean efficiencyAnomaly = isAnomalous(saved, efficiencyValue);
        // OR: conservar anomalía previa (ej. mismatch de costo) y sumar la de eficiencia
        boolean isAnomaly = Boolean.TRUE.equals(saved.getIsAnomaly()) || efficiencyAnomaly;

        saved.setEfficiencyValue(efficiencyValue);
        saved.setEfficiencyUnit(efficiencyUnit);
        saved.setIsAnomaly(isAnomaly);
        saved = fuelLogRepository.save(saved);

        if (efficiencyAnomaly) {
            notificationService.notifyFuelAnomaly(
                    "Anomalía de consumo detectada: " + saved.getAssetType().name() +
                    " ID " + saved.getAssetId() + " | " +
                    efficiencyValue.setScale(2, RoundingMode.HALF_UP) + " " + efficiencyUnit);
            log.warn("Fuel anomaly detected for {} id={}: efficiency={} {}",
                    saved.getAssetType(), saved.getAssetId(), efficiencyValue, efficiencyUnit);
        }

        return saved;
    }

    private boolean isAnomalous(FuelLogEntity saved, BigDecimal currentEfficiency) {
        List<BigDecimal> history = fuelLogRepository.findRecentEfficiencies(
                saved.getAssetType(), saved.getAssetId(), saved.getId(),
                PageRequest.of(0, 5));

        if (history.size() < 2) {
            return false;
        }

        BigDecimal avg = history.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size()), 4, RoundingMode.HALF_UP);

        if (avg.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        BigDecimal deviation = currentEfficiency.subtract(avg).abs()
                .divide(avg, 4, RoundingMode.HALF_UP);

        return deviation.compareTo(new BigDecimal("0.20")) > 0;
    }

    private void validateContinuity(AssetType assetType, FuelLogRequest request, FuelLogEntity previous) {
        if (previous == null) return;

        if (assetType == AssetType.VEHICLE || assetType == AssetType.MOTO) {
            if (request.odometerKm() != null && previous.getOdometerKm() != null) {
                if (request.odometerKm().compareTo(previous.getOdometerKm()) <= 0) {
                    throw new BadRequestException(
                            "El odómetro ingresado (" + request.odometerKm() + " km) debe ser mayor al último registrado (" +
                            previous.getOdometerKm() + " km)");
                }
            }
        } else if (assetType == AssetType.MACHINE) {
            if (request.hourMeter() != null && previous.getHourMeter() != null) {
                if (request.hourMeter().compareTo(previous.getHourMeter()) <= 0) {
                    throw new BadRequestException(
                            "El horómetro ingresado (" + request.hourMeter() + " h) debe ser mayor al último registrado (" +
                            previous.getHourMeter() + " h)");
                }
            }
        }
    }

    private void validateMaxQuantity(AssetType assetType, Long assetId, BigDecimal quantityLiters) {
        double inputGal = quantityLiters.doubleValue() / 3.785411784;

        // Primero intentar usar la capacidad específica del activo
        java.math.BigDecimal specificCapacity = resolveAssetTankCapacity(assetType, assetId);
        if (specificCapacity != null) {
            if (inputGal > specificCapacity.doubleValue()) {
                throw new BadRequestException(String.format(
                        "Cantidad (%.3f Gal) supera la capacidad del tanque configurada para este activo (%.3f Gal). " +
                        "Un ADMIN puede actualizar la capacidad del tanque en la ficha del activo.",
                        inputGal, specificCapacity.doubleValue()));
            }
            return;
        }

        // Fallback: límites globales por defecto (en litros)
        double maxLiters = switch (assetType) {
            case MACHINE -> maxQuantityMachine;
            case VEHICLE -> maxQuantityVehicle;
            case MOTO    -> maxQuantityMoto;
        };
        if (quantityLiters.doubleValue() > maxLiters) {
            double maxGal = maxLiters / 3.785411784;
            throw new BadRequestException(String.format(
                    "Cantidad de combustible (%.3f Gal) supera el máximo global para %s (%.3f Gal). " +
                    "Un ADMIN puede definir la capacidad exacta del tanque en la ficha del activo.",
                    inputGal, assetType.name(), maxGal));
        }
    }

    private java.math.BigDecimal resolveAssetTankCapacity(AssetType assetType, Long assetId) {
        return switch (assetType) {
            case MACHINE -> machineRepository.findById(assetId)
                    .map(m -> m.getFuelTankCapacityGallons()).orElse(null);
            case VEHICLE, MOTO -> vehicleRepository.findById(assetId.intValue())
                    .map(v -> v.getFuelTankCapacityGallons()).orElse(null);
        };
    }

    private void validateAssetExists(AssetType assetType, Long assetId) {
        switch (assetType) {
            case MACHINE -> machineRepository.findById(assetId)
                    .filter(m -> Boolean.TRUE.equals(m.getStatus()))
                    .orElseThrow(() -> new ResourceNotFoundException("Máquina no encontrada con ID: " + assetId));
            case VEHICLE, MOTO -> vehicleRepository.findById(assetId.intValue())
                    .filter(v -> v.getActivo() != null && v.getActivo())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con ID: " + assetId));
        }
    }

    private boolean detectCostMismatch(BigDecimal calculated, BigDecimal actual) {
        if (actual == null || calculated == null || calculated.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        BigDecimal deviation = calculated.subtract(actual).abs()
                .divide(calculated, 4, RoundingMode.HALF_UP);
        return deviation.compareTo(new BigDecimal("0.01")) > 0;
    }

    private BigDecimal computeDeltaKm(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) return null;
        BigDecimal delta = current.subtract(previous);
        return delta.compareTo(BigDecimal.ZERO) > 0 ? delta : null;
    }

    private BigDecimal computeDeltaHours(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) return null;
        BigDecimal delta = current.subtract(previous);
        return delta.compareTo(BigDecimal.ZERO) > 0 ? delta : null;
    }

    private List<FuelLogEntity> fetchLogs(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null) {
            return fuelLogRepository.findAllByDateRange(from, to);
        }
        return fuelLogRepository.findAllByOrderByFuelDateTimeDesc();
    }

    private FuelLogEntity findOrThrow(Long id) {
        return fuelLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de combustible no encontrado con ID: " + id));
    }

    private static final BigDecimal LITERS_PER_GALLON = BigDecimal.valueOf(3.785411784);

    private BigDecimal litersToGallons(BigDecimal liters) {
        if (liters == null) return null;
        return liters.divide(LITERS_PER_GALLON, 3, RoundingMode.HALF_UP);
    }

    private FuelLogResponse toResponse(FuelLogEntity e) {
        return new FuelLogResponse(
                e.getId(),
                e.getAssetType() != null ? e.getAssetType().name() : null,
                e.getAssetId(),
                e.getAssetPlate(),
                e.getFuelDateTime(),
                e.getOdometerKm(),
                e.getHourMeter(),
                e.getPreviousOdometer(),
                e.getPreviousHourMeter(),
                e.getDeltaKm(),
                e.getDeltaHours(),
                e.getQuantity(),
                e.getQuantityUnit() != null ? e.getQuantityUnit().name() : null,
                litersToGallons(e.getQuantityLiters()),
                e.getPricePerUnit(),
                e.getTotalCostCalculated(),
                e.getTotalCostActual(),
                e.getTotalCostMismatch(),
                e.getFuelType() != null ? e.getFuelType().name() : null,
                e.getServiceStation(),
                e.getIsFullTank(),
                e.getDiscountAmount(),
                e.getInvoicePhotoUrl(),
                e.getInvoiceStatus() != null ? e.getInvoiceStatus().name() : null,
                e.getVoucherNumber(),
                e.getNotes(),
                e.getEfficiencyValue(),
                e.getEfficiencyUnit(),
                e.getIsAnomaly(),
                e.getRegisteredBy(),
                e.getCreatedAt(),
                e.getAnomalyDismissedBy(),
                e.getAnomalyDismissedAt(),
                e.getAnomalyDismissReason(),
                null, null  // factoryEfficiency solo se puebla en el ranking
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GetFuelDashboardUseCase — monthly stats
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public List<FuelMonthlyStatsResponse> getMonthlyStats(LocalDateTime from, LocalDateTime to) {
        List<FuelLogEntity> logs = fetchLogs(from, to);

        record MonthKey(int year, int month) {}

        String[] names = {"", "Ene", "Feb", "Mar", "Abr", "May", "Jun",
                          "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

        Map<MonthKey, List<FuelLogEntity>> grouped = logs.stream()
                .collect(Collectors.groupingBy(f -> new MonthKey(
                        f.getFuelDateTime().getYear(),
                        f.getFuelDateTime().getMonthValue())));

        return grouped.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<MonthKey, List<FuelLogEntity>> e) -> e.getKey().year())
                        .thenComparingInt(e -> e.getKey().month()))
                .map(e -> {
                    MonthKey k = e.getKey();
                    List<FuelLogEntity> ml = e.getValue();
                    List<FuelLogEntity> machine = ml.stream().filter(f -> f.getAssetType() == AssetType.MACHINE).toList();
                    List<FuelLogEntity> vehicle = ml.stream().filter(f -> f.getAssetType() == AssetType.VEHICLE).toList();
                    List<FuelLogEntity> moto    = ml.stream().filter(f -> f.getAssetType() == AssetType.MOTO).toList();
                    return new FuelMonthlyStatsResponse(
                            k.year(), k.month(),
                            names[k.month()] + " " + k.year(),
                            litersToGallons(sumField(ml, FuelLogEntity::getQuantityLiters)),
                            sumField(ml, FuelLogEntity::getTotalCostCalculated),
                            litersToGallons(sumField(machine, FuelLogEntity::getQuantityLiters)),
                            sumField(machine, FuelLogEntity::getTotalCostCalculated),
                            machine.size(),
                            litersToGallons(sumField(vehicle, FuelLogEntity::getQuantityLiters)),
                            sumField(vehicle, FuelLogEntity::getTotalCostCalculated),
                            vehicle.size(),
                            litersToGallons(sumField(moto, FuelLogEntity::getQuantityLiters)),
                            sumField(moto, FuelLogEntity::getTotalCostCalculated),
                            moto.size(),
                            ml.size(),
                            ml.stream().filter(l -> Boolean.TRUE.equals(l.getIsAnomaly())).count()
                    );
                })
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Exportación Excel
    // ──────────────────────────────────────────────────────────────────────────

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public byte[] exportToExcel(LocalDateTime from, LocalDateTime to) throws IOException {
        List<FuelLogEntity> logs = fetchLogs(from, to);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = buildHeaderStyle(wb);
            CellStyle dataStyle   = buildDataStyle(wb);

            Sheet sheet = wb.createSheet("Combustibles");

            String[] headers = {
                "ID", "Tipo Activo", "ID Activo", "Placa",
                "Fecha/Hora", "Odómetro (km)", "Horómetro (h)",
                "Cantidad", "Unidad", "Gal equiv.", "Precio/Unidad",
                "Costo Calculado", "Costo Real", "Descuento",
                "Tipo Combustible", "Estación", "Tanque Lleno",
                "Eficiencia", "Unidad Efic.", "Anomalía",
                "Estado Factura", "N° Tiquete", "Notas", "Registrado Por"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (FuelLogEntity e : logs) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                setCell(row, col++, e.getId(), dataStyle);
                setCell(row, col++, e.getAssetType() != null ? e.getAssetType().name() : "", dataStyle);
                setCell(row, col++, e.getAssetId(), dataStyle);
                setCell(row, col++, e.getAssetPlate(), dataStyle);
                setCell(row, col++, e.getFuelDateTime() != null ? e.getFuelDateTime().format(DT_FMT) : "", dataStyle);
                setCell(row, col++, e.getOdometerKm(), dataStyle);
                setCell(row, col++, e.getHourMeter(), dataStyle);
                setCell(row, col++, e.getQuantity(), dataStyle);
                setCell(row, col++, e.getQuantityUnit() != null ? e.getQuantityUnit().name() : "", dataStyle);
                setCell(row, col++, litersToGallons(e.getQuantityLiters()), dataStyle);
                setCell(row, col++, e.getPricePerUnit(), dataStyle);
                setCell(row, col++, e.getTotalCostCalculated(), dataStyle);
                setCell(row, col++, e.getTotalCostActual(), dataStyle);
                setCell(row, col++, e.getDiscountAmount(), dataStyle);
                setCell(row, col++, e.getFuelType() != null ? e.getFuelType().name() : "", dataStyle);
                setCell(row, col++, e.getServiceStation(), dataStyle);
                setCell(row, col++, Boolean.TRUE.equals(e.getIsFullTank()) ? "Sí" : "No", dataStyle);
                setCell(row, col++, e.getEfficiencyValue(), dataStyle);
                setCell(row, col++, e.getEfficiencyUnit(), dataStyle);
                setCell(row, col++, Boolean.TRUE.equals(e.getIsAnomaly()) ? "Sí" : "No", dataStyle);
                setCell(row, col++, e.getInvoiceStatus() != null ? e.getInvoiceStatus().name() : "", dataStyle);
                setCell(row, col++, e.getVoucherNumber(), dataStyle);
                setCell(row, col++, e.getNotes(), dataStyle);
                setCell(row, col, e.getRegisteredBy(), dataStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle buildDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void setCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
        } else if (value instanceof Long l) {
            cell.setCellValue(l);
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private BigDecimal sumField(List<FuelLogEntity> list,
                                java.util.function.Function<FuelLogEntity, BigDecimal> mapper) {
        return list.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private FuelLogResponse toRankingResponse(FuelLogEntity rep, BigDecimal totalGallons,
                                               BigDecimal totalCost, int recordCount) {
        // Leer eficiencia de fábrica del activo para mostrar junto a la estimada
        BigDecimal factoryEff = null;
        String factoryEffUnit = null;
        try {
            if (rep.getAssetType() == AssetType.MACHINE) {
                var machine = machineRepository.findById(rep.getAssetId()).orElse(null);
                if (machine != null && machine.getFactoryEfficiencyGalPerHour() != null) {
                    factoryEff = machine.getFactoryEfficiencyGalPerHour();
                    factoryEffUnit = machine.getFactoryEfficiencyUnit() != null
                            ? machine.getFactoryEfficiencyUnit() : "GAL_PER_HOUR";
                }
            } else {
                var vehicle = vehicleRepository.findById(rep.getAssetId().intValue()).orElse(null);
                if (vehicle != null && vehicle.getFactoryEfficiencyKmPerGallon() != null) {
                    factoryEff = vehicle.getFactoryEfficiencyKmPerGallon();
                    factoryEffUnit = vehicle.getFactoryEfficiencyUnit() != null
                            ? vehicle.getFactoryEfficiencyUnit() : "KM_PER_GALLON";
                }
            }
        } catch (Exception ignored) {}

        return new FuelLogResponse(
                rep.getId(),
                rep.getAssetType() != null ? rep.getAssetType().name() : null,
                rep.getAssetId(),
                rep.getAssetPlate(),
                rep.getFuelDateTime(),
                null, null, null, null, null, null,
                null,
                rep.getQuantityUnit() != null ? rep.getQuantityUnit().name() : null,
                totalGallons,
                null,
                totalCost,
                null, null,
                rep.getFuelType() != null ? rep.getFuelType().name() : null,
                null, null, null, null, null, null, null,
                rep.getEfficiencyValue(),
                rep.getEfficiencyUnit(),
                null, null, null,
                null, null, null,  // dismissal fields
                factoryEff, factoryEffUnit
        );
    }

    private String resolveUsername() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserPrincipal up) {
                return up.username();
            }
        } catch (Exception ignored) {}
        return "anonymous";
    }

    private AssetType parseAssetType(String value) {
        try {
            return AssetType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo de activo inválido: " + value);
        }
    }

    private QuantityUnit parseQuantityUnit(String value) {
        try {
            return value != null ? QuantityUnit.valueOf(value.toUpperCase()) : QuantityUnit.GALLONS;
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unidad de cantidad inválida: " + value);
        }
    }

    private FuelType parseFuelType(String value) {
        try {
            return FuelType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo de combustible inválido: " + value);
        }
    }
}
