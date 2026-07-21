package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelLogResponse;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface GetFuelHistoryUseCase {
    List<FuelLogResponse> getHistoryByAsset(String assetType, Long assetId);
    List<FuelLogResponse> getHistoryByAssetAndDateRange(String assetType, Long assetId, LocalDateTime from, LocalDateTime to);
    FuelLogResponse getById(Long id);
    FuelLogResponse updateInvoiceStatus(Long id, String status);
    FuelLogResponse uploadInvoiceFile(Long id, MultipartFile file);
    FuelLogResponse uploadInvoiceFileTemporal(MultipartFile file);
    FuelLogResponse dismissAnomaly(Long id, String reason, BigDecimal correctedCost);
}
