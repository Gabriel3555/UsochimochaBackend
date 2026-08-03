package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordRequest;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.application.port.AdjustFuelInventoryUseCase;
import com.app.usochicamochabackend.fuel.application.port.RegisterRefuelingRecordUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefuelingRecordService implements RegisterRefuelingRecordUseCase {

    private static final String LUGAR_BOMBA = "BOMBA";
    private static final String LUGAR_ALMACEN = "ALMACEN";

    private final RefuelingRecordsRepository refuelingRecordsRepository;
    private final AdjustFuelInventoryUseCase adjustFuelInventoryUseCase;
    private final FuelDocumentStorageService fuelDocumentStorageService;
    private final SaveActionUseCase saveActionUseCase;
    private final VehicleRepository vehicleRepository;
    private final MachineRepository machineRepository;
    private final AssetFuelCapacityService assetFuelCapacityService;

    @Value("${app.fuel.discrepancia-tolerancia-porcentaje:0.01}")
    private BigDecimal tolerancia;

    @Override
    @Transactional
    public RefuelingRecordResponse registrar(RefuelingRecordRequest request, MultipartFile factura, Long responsableId) {
        validar(request);
        boolean tieneVehiculo = request.vehicleId() != null;
        boolean esBomba = request.lugar().equals(LUGAR_BOMBA);

        BigDecimal totalCalculado = null;
        Boolean discrepanciaValor = false;

        if (esBomba) {
            if (request.precioUnitario() == null || factura == null || factura.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Un tanqueo en BOMBA requiere precioUnitario y factura.");
            }
            if (request.totalIngresado() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "totalIngresado es obligatorio en BOMBA.");
            }
            TotalYDiscrepancia calculo = calcularTotalYDiscrepancia(
                    request.cantidadGalones(), request.precioUnitario(), request.descuento(), request.totalIngresado());
            totalCalculado = calculo.totalCalculado();
            discrepanciaValor = calculo.discrepanciaValor();
        } else {
            // ALMACEN: valida y descuenta stock ANTES de persistir (lanza 409 si no alcanza).
            adjustFuelInventoryUseCase.decrement(request.areaCosto(), request.fuelTypeId(), request.cantidadGalones());
        }

        RefuelingRecordsEntity entity = RefuelingRecordsEntity.builder()
                .vehicleId(request.vehicleId())
                .machineId(request.machineId())
                .lugar(request.lugar())
                .areaCosto(request.areaCosto())
                .fuelTypeId(request.fuelTypeId())
                .cantidadGalones(request.cantidadGalones())
                .horometroKm(request.horometroKm())
                .esFull(request.esFull() != null ? request.esFull() : false)
                .precioUnitario(request.precioUnitario())
                .descuento(request.descuento())
                .totalIngresado(request.totalIngresado())
                .totalCalculado(totalCalculado)
                .discrepanciaValor(discrepanciaValor)
                // Placeholder no nulo cuando lugar=BOMBA: el CHECK de la migración V20
                // (`lugar <> 'BOMBA' OR url_factura IS NOT NULL`) se evalúa en este INSERT,
                // antes de que exista el id necesario para subir el archivo real. Mismo
                // patrón que ya usa FuelPurchaseService para las facturas de compra.
                .urlFactura(esBomba ? "pendiente" : null)
                .origen(request.origen())
                .responsableId(responsableId)
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.now()))
                .status(true)
                .build();
        entity = refuelingRecordsRepository.save(entity);

        if (esBomba) {
            try {
                String urlFactura = fuelDocumentStorageService.store(factura, "refueling", entity.getId());
                entity.setUrlFactura(urlFactura);
                entity = refuelingRecordsRepository.save(entity);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la factura.", e);
            }
        }

        actualizarLecturaActual(request.vehicleId(), request.machineId(), request.horometroKm());

        String activo = tieneVehiculo ? ("vehículo id=" + request.vehicleId()) : ("máquina id=" + request.machineId());
        saveActionUseCase.save("Se registró un tanqueo de " + request.cantidadGalones()
                + " galones en " + request.lugar() + " para " + activo);

        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public RefuelingRecordResponse actualizar(Long id, RefuelingRecordRequest request, MultipartFile factura) {
        RefuelingRecordsEntity entity = refuelingRecordsRepository.findByIdAndStatus(id, true)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tanqueo no encontrado."));

        validar(request);

        boolean lugarEraAlmacen = LUGAR_ALMACEN.equals(entity.getLugar());
        boolean lugarEsAlmacen = LUGAR_ALMACEN.equals(request.lugar());
        boolean esBomba = LUGAR_BOMBA.equals(request.lugar());

        // Revierte el efecto de inventario viejo (si el tanqueo era ALMACEN) y aplica
        // el nuevo (si lo sigue siendo o pasa a serlo) — correcto ante cualquier
        // combinación de cambios (activo, área, combustible, cantidad o el propio
        // lugar), sin necesidad de calcular deltas caso por caso.
        if (lugarEraAlmacen) {
            adjustFuelInventoryUseCase.increment(entity.getAreaCosto(), entity.getFuelTypeId(), entity.getCantidadGalones());
        }
        if (lugarEsAlmacen) {
            adjustFuelInventoryUseCase.decrement(request.areaCosto(), request.fuelTypeId(), request.cantidadGalones());
        }

        BigDecimal totalCalculado = null;
        Boolean discrepanciaValor = false;
        if (esBomba) {
            if (request.precioUnitario() == null || request.totalIngresado() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "precioUnitario y totalIngresado son obligatorios cuando lugar es BOMBA.");
            }
            TotalYDiscrepancia calculo = calcularTotalYDiscrepancia(
                    request.cantidadGalones(), request.precioUnitario(), request.descuento(), request.totalIngresado());
            totalCalculado = calculo.totalCalculado();
            discrepanciaValor = calculo.discrepanciaValor();

            // Solo era null si el tanqueo no era BOMBA todavía (ALMACEN nunca guarda
            // url_factura) — si ya era BOMBA, siempre tiene una factura real persistida.
            boolean necesitaFacturaNueva = entity.getUrlFactura() == null;
            if (necesitaFacturaNueva && (factura == null || factura.isEmpty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Se requiere factura al cambiar un tanqueo a BOMBA.");
            }
            if (entity.getUrlFactura() == null) {
                entity.setUrlFactura("pendiente"); // mismo placeholder que registrar(), por el CHECK de la V20
            }
        }

        entity.setVehicleId(request.vehicleId());
        entity.setMachineId(request.machineId());
        entity.setLugar(request.lugar());
        entity.setAreaCosto(request.areaCosto());
        entity.setFuelTypeId(request.fuelTypeId());
        entity.setCantidadGalones(request.cantidadGalones());
        entity.setHorometroKm(request.horometroKm());
        entity.setEsFull(request.esFull() != null ? request.esFull() : false);
        entity.setPrecioUnitario(request.precioUnitario());
        entity.setDescuento(request.descuento());
        entity.setTotalIngresado(request.totalIngresado());
        entity.setTotalCalculado(totalCalculado);
        entity.setDiscrepanciaValor(discrepanciaValor);
        entity.setOrigen(request.origen());
        entity = refuelingRecordsRepository.save(entity);

        if (esBomba && factura != null && !factura.isEmpty()) {
            try {
                String urlFactura = fuelDocumentStorageService.store(factura, "refueling", entity.getId());
                entity.setUrlFactura(urlFactura);
                entity = refuelingRecordsRepository.save(entity);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la factura.", e);
            }
        }

        actualizarLecturaActual(entity.getVehicleId(), entity.getMachineId(), entity.getHorometroKm());

        auditar("Se editó el tanqueo id=" + id);
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        RefuelingRecordsEntity entity = refuelingRecordsRepository.findByIdAndStatus(id, true)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tanqueo no encontrado."));

        if (LUGAR_ALMACEN.equals(entity.getLugar())) {
            adjustFuelInventoryUseCase.increment(entity.getAreaCosto(), entity.getFuelTypeId(), entity.getCantidadGalones());
        }
        entity.setStatus(false);
        refuelingRecordsRepository.save(entity);

        auditar("Se eliminó el tanqueo id=" + id);
    }

    private void validar(RefuelingRecordRequest request) {
        boolean tieneVehiculo = request.vehicleId() != null;
        boolean tieneMaquina = request.machineId() != null;
        if (tieneVehiculo == tieneMaquina) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar exactamente uno de vehicleId o machineId.");
        }
        if (request.lugar() == null || (!request.lugar().equals(LUGAR_BOMBA) && !request.lugar().equals(LUGAR_ALMACEN))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lugar debe ser BOMBA o ALMACEN.");
        }
        if (request.areaCosto() == null || request.fuelTypeId() == null || request.cantidadGalones() == null
                || request.horometroKm() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "areaCosto, fuelTypeId, cantidadGalones y horometroKm son obligatorios.");
        }
    }

    private record TotalYDiscrepancia(BigDecimal totalCalculado, boolean discrepanciaValor) {}

    private TotalYDiscrepancia calcularTotalYDiscrepancia(BigDecimal cantidad, BigDecimal precioUnitario,
                                                            BigDecimal descuento, BigDecimal totalIngresado) {
        BigDecimal desc = descuento != null ? descuento : BigDecimal.ZERO;
        BigDecimal totalCalculado = cantidad.multiply(precioUnitario).subtract(desc);
        boolean discrepancia = totalCalculado.subtract(totalIngresado).abs()
                .compareTo(totalCalculado.multiply(tolerancia)) > 0;
        return new TotalYDiscrepancia(totalCalculado, discrepancia);
    }

    // Mismo patrón que VehicleOilChangeService: el horómetro/km del tanqueo pasa a
    // ser la lectura "actual" del vehículo/máquina (la que usa Inventario, alertas,
    // etc.) solo si es mayor a la que ya tenía — nunca la hace retroceder.
    private void actualizarLecturaActual(Integer vehicleId, Long machineId, BigDecimal horometroKm) {
        int nuevaLectura = horometroKm.setScale(0, RoundingMode.HALF_UP).intValue();
        if (vehicleId != null) {
            vehicleRepository.findById(vehicleId).ifPresent(vehicle -> {
                int actual = vehicle.getKilometrajeActual() != null ? vehicle.getKilometrajeActual() : 0;
                if (nuevaLectura > actual) {
                    vehicle.setKilometrajeActual(nuevaLectura);
                    vehicle.setFechaUltimoReporte(LocalDateTime.now());
                    vehicleRepository.save(vehicle);
                }
            });
        } else {
            machineRepository.findById(machineId).ifPresent(machine -> {
                int actual = machine.getHorometroActual() != null ? machine.getHorometroActual() : 0;
                if (nuevaLectura > actual) {
                    machine.setHorometroActual(nuevaLectura);
                    machineRepository.save(machine);
                }
            });
        }
    }

    private void auditar(String mensajeBase) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save(mensajeBase + " por " + userPrincipal.username());
        } else {
            saveActionUseCase.save(mensajeBase);
        }
    }

    @Override
    public Page<RefuelingRecordResponse> listar(Pageable pageable, Boolean activo) {
        Page<RefuelingRecordsEntity> page = activo != null
                ? refuelingRecordsRepository.findByStatus(activo, pageable)
                : refuelingRecordsRepository.findAll(pageable);
        return page.map(this::mapToResponse);
    }

    private RefuelingRecordResponse mapToResponse(RefuelingRecordsEntity entity) {
        boolean capacidadExcedida = assetFuelCapacityService.excedeCapacidad(
                entity.getVehicleId(), entity.getMachineId(), entity.getCantidadGalones());
        return RefuelingRecordResponse.from(entity, capacidadExcedida);
    }
}
