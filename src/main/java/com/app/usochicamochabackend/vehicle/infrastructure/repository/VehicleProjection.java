package com.app.usochicamochabackend.vehicle.infrastructure.repository;

public interface VehicleProjection {
    Integer getId();

    String getPlaca();

    String getMarca();

    Integer getIdMarca();

    Integer getIdTipoVehiculo();

    String getTipoVehiculo();

    Integer getKilometrajeActual();
    String getBelongsTo();

    Integer getIdUbicacionBase();

    String getUbicacionBase();
}
