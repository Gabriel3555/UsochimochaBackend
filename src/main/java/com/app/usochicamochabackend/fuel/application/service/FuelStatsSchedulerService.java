package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetType;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelLogEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelStatsMonthlyEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelType;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelLogRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelStatsMonthlyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FuelStatsSchedulerService {

    private static final BigDecimal LITERS_PER_GALLON = BigDecimal.valueOf(3.785411784);

    private final FuelLogRepository fuelLogRepository;
    private final FuelStatsMonthlyRepository statsRepository;

    @Scheduled(cron = "0 0 2 * * *")
    public void materializeMonthlyStats() {
        YearMonth prev = YearMonth.now().minusMonths(1);
        YearMonth curr = YearMonth.now();
        materializeMonth(prev);
        materializeMonth(curr);
        log.info("FuelStatsSchedulerService: materialized stats for {} and {}", prev, curr);
    }

    @Transactional
    public void materializeMonth(YearMonth yearMonth) {
        String ymKey = yearMonth.toString();
        LocalDateTime from = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime to = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<FuelLogEntity> logs = fuelLogRepository.findAllByDateRange(from, to);
        if (logs.isEmpty()) return;

        Map<String, List<FuelLogEntity>> grouped = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getAssetType().name() + "|" + l.getFuelType().name()));

        for (Map.Entry<String, List<FuelLogEntity>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            AssetType assetType = AssetType.valueOf(parts[0]);
            FuelType fuelType = FuelType.valueOf(parts[1]);
            List<FuelLogEntity> group = entry.getValue();

            BigDecimal totalGallons = group.stream()
                    .map(FuelLogEntity::getQuantityLiters)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(LITERS_PER_GALLON, 4, RoundingMode.HALF_UP);

            BigDecimal totalCost = group.stream()
                    .map(FuelLogEntity::getTotalCostCalculated)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int anomalyCount = (int) group.stream()
                    .filter(l -> Boolean.TRUE.equals(l.getIsAnomaly())).count();

            FuelStatsMonthlyEntity stat = statsRepository
                    .findByYearMonthAndAssetTypeAndFuelType(ymKey, assetType, fuelType)
                    .orElse(FuelStatsMonthlyEntity.builder()
                            .yearMonth(ymKey)
                            .assetType(assetType)
                            .fuelType(fuelType)
                            .build());

            stat.setTotalGallons(totalGallons);
            stat.setTotalCost(totalCost);
            stat.setTotalLoads(group.size());
            stat.setAnomalyCount(anomalyCount);
            stat.setUpdatedAt(LocalDateTime.now());

            statsRepository.save(stat);
        }
    }
}
