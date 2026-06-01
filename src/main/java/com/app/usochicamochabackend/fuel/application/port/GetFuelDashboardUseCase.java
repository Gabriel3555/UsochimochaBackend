package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelDashboardResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelLogResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelMonthlyStatsResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public interface GetFuelDashboardUseCase {
    FuelDashboardResponse getDashboard(LocalDateTime from, LocalDateTime to);
    List<FuelLogResponse> getAllLogs(LocalDateTime from, LocalDateTime to);
    List<FuelLogResponse> getAnomalies();
    List<FuelLogResponse> getRanking(LocalDateTime from, LocalDateTime to);
    List<FuelMonthlyStatsResponse> getMonthlyStats(LocalDateTime from, LocalDateTime to);
    List<FuelMonthlyStatsResponse> getHistoricalStats(LocalDateTime from, LocalDateTime to);
    byte[] exportToExcel(LocalDateTime from, LocalDateTime to) throws IOException;
}
