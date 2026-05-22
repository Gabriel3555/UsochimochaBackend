package com.app.usochicamochabackend.review.application.port;

public interface UpdateInspectionHourMeterUseCase {
    void updateHourMeter(Long machineId, Double newHourMeter);
}
