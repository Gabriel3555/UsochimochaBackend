package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordRequest;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.application.port.RegisterRefuelingRecordUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelReintegrationsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelReintegrationsRepository;
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
    private final FuelDocumentStorageService fuelDocumentStorageService;
    private final SaveActionUseCase saveActionUseCase;
    private final VehicleRepository vehicleRepository;
    private final MachineRepository machineRepository;
    private final AssetFuelCapacityService assetFuelCapacityService;
    private final FuelPriceAnomalyService fuelPriceAnomalyService;
    private final FuelFullConsistencyService fuelFullConsistencyService;
    private final FuelReintegrationsRepository fuelReintegrationsRepository;

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

        if (esBomba && request.precioUnitario() != null && request.totalIngresado() != null) {
            // Precio unitario y total pagado son opcionales (a los operarios no les
            // interesa tanto el costo por tanqueo individual — llega un informe
            // mensual aparte; el foco real está en Rendimiento). Si no vienen, el
            // tanqueo se registra igual, solo sin totalCalculado/discrepancia.
            TotalYDiscrepancia calculo = calcularTotalYDiscrepancia(
                    request.cantidadGalones(), request.precioUnitario(), request.descuento(), request.totalIngresado());
            totalCalculado = calculo.totalCalculado();
            discrepanciaValor = calculo.discrepanciaValor();
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
                // Factura opcional (V23): ya no hace falta el placeholder "pendiente" que
                // exigía el CHECK original de la V20 — null es válido incluso en BOMBA.
                .urlFactura(null)
                .origen(request.origen())
                .responsableId(responsableId)
                // Fecha real del tanqueo, opcional: si no viene, se usa el momento del
                // registro (comportamiento anterior). Permite registrar tanqueos tarde
                // sin perder el histórico real (ver actualizar()).
                .fechaRegistro(Timestamp.valueOf(request.fecha() != null ? request.fecha() : LocalDateTime.now()))
                .status(true)
                .build();
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

        boolean esBomba = LUGAR_BOMBA.equals(request.lugar());

        BigDecimal totalCalculado = null;
        Boolean discrepanciaValor = false;
        if (esBomba && request.precioUnitario() != null && request.totalIngresado() != null) {
            // Precio unitario y total pagado son opcionales (ver nota en registrar()).
            TotalYDiscrepancia calculo = calcularTotalYDiscrepancia(
                    request.cantidadGalones(), request.precioUnitario(), request.descuento(), request.totalIngresado());
            totalCalculado = calculo.totalCalculado();
            discrepanciaValor = calculo.discrepanciaValor();
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
        // Permite corregir la fecha de un tanqueo registrado tarde; si no viene,
        // conserva la fecha que ya tenía (no la pisa con "ahora").
        if (request.fecha() != null) {
            entity.setFechaRegistro(Timestamp.valueOf(request.fecha()));
        }
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
        boolean cantidadFueraDeRango = assetFuelCapacityService.cantidadFueraDeRangoTipico(
                entity.getVehicleId(), entity.getMachineId(), entity.getCantidadGalones());
        boolean precioFueraDeRango = fuelPriceAnomalyService.precioFueraDeRango(
                entity.getFuelTypeId(), entity.getPrecioUnitario(), entity.getId());
        BigDecimal cantidadReintegrada = sumaReintegros(entity.getId());
        BigDecimal capacidadConfiguradaGal = assetFuelCapacityService.capacidadConfigurada(
                entity.getVehicleId(), entity.getMachineId());
        BigDecimal cantidadMaximaTipica = assetFuelCapacityService.maximoTipico(
                entity.getVehicleId(), entity.getMachineId());
        BigDecimal precioPromedioReciente = fuelPriceAnomalyService.promedioReciente(
                entity.getFuelTypeId(), entity.getId());
        boolean fullInconsistente = fuelFullConsistencyService.fullInconsistente(
                entity.getVehicleId(), entity.getMachineId(), entity.getEsFull(), entity.getCantidadGalones(),
                entity.getHorometroKm(), entity.getFechaRegistro(), capacidadConfiguradaGal);
        BigDecimal cantidadEsperadaLlenoGal = fuelFullConsistencyService.cantidadEsperadaParaLleno(
                entity.getVehicleId(), entity.getMachineId(), entity.getEsFull(), entity.getHorometroKm(),
                entity.getFechaRegistro(), capacidadConfiguradaGal);
        // Resolver nombre del activo (vehículo/máquina/moto) para auditoría
        String assetName = entity.getVehicleId() != null ?
                vehicleRepository.findById(entity.getVehicleId())
                        .map(v -> v.getPlaca()).orElse("Vehículo #" + entity.getVehicleId()) :
                machineRepository.findById(entity.getMachineId())
                        .map(m -> m.getName()).orElse("Máquina #" + entity.getMachineId());
        String assetType = entity.getVehicleId() != null ? "Vehículo" : "Máquina";

        return RefuelingRecordResponse.from(entity, capacidadExcedida, cantidadFueraDeRango, precioFueraDeRango,
                fullInconsistente, cantidadReintegrada, capacidadConfiguradaGal, cantidadMaximaTipica,
                precioPromedioReciente, cantidadEsperadaLlenoGal, assetName, assetType);
    }

    private BigDecimal sumaReintegros(Long refuelingId) {
        return fuelReintegrationsRepository.findByRefuelingId(refuelingId).stream()
                .map(FuelReintegrationsEntity::getCantidadReintegrada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
