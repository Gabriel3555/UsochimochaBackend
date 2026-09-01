package com.app.usochicamochabackend.moto.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.common.text.InputTextNormalizer;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.exception.VehicleSoftDeletedConflictException;
import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import com.app.usochicamochabackend.catalog.infrastructure.entity.UbicacionEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.TipoVehiculoRepository;
import com.app.usochicamochabackend.catalog.infrastructure.repository.UbicacionRepository;
import com.app.usochicamochabackend.mapper.VehicleMapper;
import com.app.usochicamochabackend.moto.application.dto.*;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.app.usochicamochabackend.notifications.application.PreventiveAlertCalculationService;
import com.app.usochicamochabackend.moto.application.port.MotoCRUDUseCase;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleRequest;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.DocumentacionYElementosEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspDetalleDocumentosEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspDetalleMecanicoEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspPreOperativaEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.DocumentacionYElementosRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspDetalleDocumentosRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspDetalleMecanicoRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspPreOperativaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MotoService implements MotoCRUDUseCase {

    private static final String TIPO_MOTO = "MOTOCICLETA";

    private final VehicleRepository vehicleRepository;
    private final UbicacionRepository ubicacionRepository;
    private final TipoVehiculoRepository tipoVehiculoRepository;
    private final InspPreOperativaRepository inspeccionRepository;
    private final InspDetalleDocumentosRepository detalleDocumentosRepository;
    private final InspDetalleMecanicoRepository detalleMecanicoRepository;
    private final DocumentacionYElementosRepository documentacionRepository;
    private final NotificationService notificationService;
    private final SaveActionUseCase saveActionUseCase;
    private final PreventiveAlertCalculationService preventiveAlertCalculationService;

        /** Retorna las motocicletas activas (tipo = MOTOCICLETA) con su ubicación base y marca */
        public List<MotoPlacaResponse> getMotocicletas() {
                log.debug("[MotoService.getMotocicletas] Buscando todas las motos activas...");
                List<MotoPlacaResponse> response = vehicleRepository.findAllActiveVehiclesByTipoName(TIPO_MOTO)
                                .stream()
                                .map(v -> {
                                        log.debug("   - Moto encontrada: ID={}, Placa={}, Marca={}", v.getId(), v.getPlaca(), v.getMarca());
                                        return new MotoPlacaResponse(v.getId(), v.getPlaca(), v.getMarca(), v.getIdUbicacionBase(), v.getUbicacionBase());
                                })
                                .toList();

                log.info("[MotoService.getMotocicletas] Total {} motos activas devueltas", response.size());
                if (response.isEmpty()) {
                        log.warn("[MotoService.getMotocicletas] Sin motos activas en el sistema");
                }

                return response;
        }

        /** Retorna todas las ubicaciones activas */
        public List<UbicacionResponse> getUbicaciones() {
                return ubicacionRepository.findByActivoTrue()
                                .stream()
                                .map(u -> new UbicacionResponse(u.getId(), u.getNombreUbicacion()))
                                .toList();
        }

        /**
         * Pre-llenado de la app: retorna el estado de los 3 documentos de la moto
         * - Fecha/Imagen: De la TABLA MAESTRA documentacion_y_elementos.
         * - Estado: De la ÚLTIMA INSPECCIÓN (insp_detalle_documentos).
         * - Fallback: Si no hay inspección previa, calcula el estado en tiempo real.
         */
        public List<DocumentoExistenteResponse> getDocumentosByPlaca(String placa) {
                String p = InputTextNormalizer.normalizePlaca(placa);
                VehicleEntity vehiculo = vehicleRepository.findByPlacaAndActivoTrue(p)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Vehículo no encontrado: " + p));

                // Definir los tipos que la APP espera y mapearlos a la Base de Datos
                return java.util.List.of("SOAT", "REVISION_TECNO", "LICENCIA").stream()
                                .map(tipoApp -> {
                                        // Mapeo: App -> DB
                                        String tipoDb = switch (tipoApp) {
                                                case "REVISION_TECNO" -> "TECNOMECANICA";
                                                case "LICENCIA" -> "LICENCIA DE CONDUCCION";
                                                default -> tipoApp;
                                        };

                                        // Buscar datos maestros (fecha, imagen)
                                        var docOpt = documentacionRepository.findLatestByVehiculoAndTipo(
                                                        vehiculo.getIdVehiculo(), tipoDb);

                                        java.time.LocalDate fechaVenc = null;
                                        String fullImagenUrl = null;
                                        String mesyear = null;

                                        if (docOpt.isPresent()) {
                                                DocumentacionYElementosEntity doc = docOpt.get();
                                                fechaVenc = doc.getFechaVencimiento();

                                                if (doc.getImagenUrl() != null && !doc.getImagenUrl().isBlank()) {
                                                        String rawUrl = doc.getImagenUrl().trim();
                                                        if (rawUrl.toLowerCase().startsWith("http")) {
                                                                fullImagenUrl = rawUrl;
                                                        } else {
                                                                // Lógica Universal: Soporta rutas completas (Web) y nombres de archivo (Manual)
                                                                String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
                                                                String cleanPath = rawUrl.replace("\\", "/");
                                                                if (cleanPath.startsWith("/")) cleanPath = cleanPath.substring(1);

                                                                if (cleanPath.toLowerCase().startsWith("uploads/")) {
                                                                        // Ya trae la ruta (subido desde la Web)
                                                                        fullImagenUrl = baseUrl + "/" + cleanPath;
                                                                } else {
                                                                        // Es solo el nombre (subido manual a carpeta documents)
                                                                        fullImagenUrl = baseUrl + "/uploads/documents/" + cleanPath;
                                                                }
                                                        }
                                                }

                                                if (fechaVenc != null) {
                                                        mesyear = fechaVenc.getYear() + "-"
                                                                        + String.format("%02d",
                                                                                        fechaVenc.getMonthValue());
                                                }
                                        }

                                        // Lógica IGUAL A VEHÍCULOS: Calcular estado siempre desde la fecha maestra
                                        String estado = calcularEstado(fechaVenc);

                                        return new DocumentoExistenteResponse(
                                                        null, tipoApp, fechaVenc, mesyear, fullImagenUrl,
                                                        vehiculo.getKilometrajeActual(),
                                                        estado);
                                })
                                .toList();
        }

        @Transactional
        public Long saveInspeccion(InspeccionMotoRequest req) {
                log.info("[MotoService.saveInspeccion] Iniciando guardado de inspección para vehículo ID: {}", req.idVehiculo());
                log.debug("   - Placa esperada: (from request)");
                log.debug("   - idUbicacion: {}", req.idUbicacion());
                log.debug("   - estadoVehiculo: {}", req.estadoVehiculo());
                log.debug("   - kilometrajeReportado: {}", req.kilometrajeReportado());

                UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getPrincipal();
                String responsable = userPrincipal.username();
                log.debug("   - Usuario responsable: {}", responsable);

                VehicleEntity vehiculo = vehicleRepository.findById(req.idVehiculo())
                                .orElseThrow(() -> {
                                        log.error("❌ [MotoService.saveInspeccion] Vehículo NO ENCONTRADO con ID: {}", req.idVehiculo());
                                        return new ResourceNotFoundException(
                                                "Vehículo no encontrado: " + req.idVehiculo());
                                });

                // Actualizar fecha del último reporte (siempre) y kilometraje (si aplica)
                if (req.kilometrajeReportado() != null && req.kilometrajeReportado() > 0) {
                        vehicleRepository.updateKilometrajeWithDate(vehiculo.getIdVehiculo(), req.kilometrajeReportado(), LocalDateTime.now());
                } else {
                        // Si no hay kilometraje, actualizar solo la fecha
                        vehiculo.setFechaUltimoReporte(LocalDateTime.now());
                        vehicleRepository.save(vehiculo);
                }

                InspPreOperativaEntity inspeccion = InspPreOperativaEntity.builder()
                                .idVehiculo(vehiculo.getIdVehiculo())
                                .fechaRegistro(LocalDateTime.now())
                                .kilometrajeReportado(req.kilometrajeReportado())
                                .estadoVehiculo(req.estadoVehiculo())
                                .idUbicacion(req.idUbicacion())
                                .observacionesFinales(req.observacionesFinales())
                                .loginUser(responsable)
                                .build();

                inspeccionRepository.save(inspeccion);

                // Recalcular alertas de inmediato: el kilometraje reportado pudo haber cruzado
                // el umbral de cambio de aceite.
                preventiveAlertCalculationService.calculateAndEmitAlerts();

                // ── 2: insp_detalle_mecanico (Estado Visual + Campos nuevos) ─────────
                detalleMecanicoRepository.save(
                                InspDetalleMecanicoEntity.builder()
                                                .idInspeccion(inspeccion.getIdInspeccion())
                                                .nivelAceite(req.checkNivelAceite())
                                                .estadoLlantas(req.checkEstadoLlantas())
                                                .lucesGeneral(req.checkEstadoLuces())
                                                .estadoVisual(req.estadoVehiculo()) // "Estado general" de la moto
                                                .build());

                // ── 3: insp_detalle_documentos ────────────────────────────────────────
                InspDetalleDocumentosEntity detalleDoc = InspDetalleDocumentosEntity.builder()
                                .idInspeccion(inspeccion.getIdInspeccion())
                                .checkSoat(req.checkSoat())
                                .checkTecno(req.checkTecno())
                                .checkExtintor(req.checkExtintor())
                                .build();
                detalleDocumentosRepository.save(detalleDoc);

                notificationService.notifyDataUpdate("moto-inspections-updated");

                return inspeccion.getIdInspeccion();
        }

        private String calcularEstado(java.time.LocalDate fechaVencimiento) {
                if (fechaVencimiento == null)
                        return "Sin Información";

                java.time.YearMonth mesVencimiento = java.time.YearMonth.from(fechaVencimiento);
                java.time.YearMonth mesActual = java.time.YearMonth.now();

                // 1. Si el mes ya pasó -> Vencido
                if (mesVencimiento.isBefore(mesActual)) {
                        return "Vencido";
                }

                // 2. Si el mes de vencimiento es HOY -> Vigente (para no bloquear hoy)
                if (mesVencimiento.equals(mesActual)) {
                        return "Vigente";
                }

                // 3. Si el mes de vencimiento es el SIGUIENTE -> Próximo a Vencer
                if (mesVencimiento.equals(mesActual.plusMonths(1))) {
                        return "Próximo a Vencer";
                }

                // 4. Si es en el futuro
                return "Vigente";
        }
    // --- CRUD Motocicletas ---

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<VehicleResponse> findAllMotos() {
        return vehicleRepository.findAllActiveVehiclesByTipoNameWithDocuments(TIPO_MOTO).stream()
                .map(VehicleMapper::toResponse)
                .toList();
    }

    public VehicleResponse findMotoByPlaca(String placa) {
        VehicleEntity moto = vehicleRepository.findByPlacaAndActivoTrue(placa)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Moto no encontrada"));
        if (!TIPO_MOTO.equalsIgnoreCase(moto.getTipoVehiculo().getNombreTipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El vehículo no es una motocicleta");
        }
        return VehicleMapper.toResponse(moto);
    }

    @Override
    @Transactional
    public VehicleResponse createMoto(VehicleRequest request) {
        VehicleRequest req = request.normalized();
        String placa = req.placa();
        if (placa.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La placa es obligatoria");
        }

        Optional<VehicleEntity> existingVehicle = vehicleRepository.findByPlaca(placa);
        if (existingVehicle.isPresent()) {
            VehicleEntity existing = existingVehicle.get();
            if (existing.getActivo()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un vehículo con esta placa");
            } else {
                throw new VehicleSoftDeletedConflictException(
                    "La motocicleta con placa " + placa +
                    " fue eliminada. Opciones: restaurar o crear con otra placa",
                    VehicleMapper.toResponse(existing)
                );
            }
        }

        TipoVehiculoEntity tipoMoto = tipoVehiculoRepository.findByNombreTipoIgnoreCase(TIPO_MOTO)
                .orElseGet(() -> {
                    log.warn("Tipo MOTOCICLETA no encontrado. Creando automáticamente...");
                    TipoVehiculoEntity newTipo = TipoVehiculoEntity.builder()
                            .nombreTipo(TIPO_MOTO)
                            .activo(true)
                            .build();
                    return tipoVehiculoRepository.save(newTipo);
                });

        if (req.idUbicacionBase() != null && !ubicacionRepository.existsById(req.idUbicacionBase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ubicación no válida");
        }
        var ubiBase = req.idUbicacionBase() != null
                ? ubicacionRepository.getReferenceById(req.idUbicacionBase())
                : null;
        VehicleEntity entity = VehicleEntity.builder()
                .placa(placa)
                .idMarca(req.idMarca())
                .idTipoVehiculo(tipoMoto.getId())
                .kilometrajeActual(req.kilometrajeActual())
                .belongsTo(req.belongsTo())
                .ubicacionBase(ubiBase)
                .activo(req.activo() != null ? req.activo() : Boolean.TRUE)
                .fuelTankCapacityGallons(req.fuelTankCapacityGallons())
                .factoryEfficiencyKmPerGallon(req.factoryEfficiencyKmPerGallon())
                .factoryEfficiencyUnit(req.factoryEfficiencyUnit())
                .build();
        vehicleRepository.save(entity);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("La motocicleta " + entity.getPlaca() + " ha sido creada por " + userPrincipal.username());
        } else {
            saveActionUseCase.save("La motocicleta " + entity.getPlaca() + " ha sido creada");
        }

        return VehicleMapper.toResponse(
                vehicleRepository.findById(entity.getIdVehiculo()).orElse(entity));
    }

    @Override
    @Transactional
    public VehicleResponse updateMoto(Integer id, VehicleRequest request) {
        VehicleRequest req = request.normalized();
        VehicleEntity entity = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Moto no encontrada"));
        assertMotoTipo(entity);

        String placa = req.placa();
        if (placa.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La placa es obligatoria");
        }
        if (!entity.getPlaca().equalsIgnoreCase(placa)
                && vehicleRepository.findByPlaca(placa).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otro vehículo con esta placa");
        }

        TipoVehiculoEntity tipoMoto = tipoVehiculoRepository.findByNombreTipoIgnoreCase(TIPO_MOTO)
                .orElseGet(() -> {
                    log.warn("Tipo MOTOCICLETA no encontrado. Creando automáticamente...");
                    TipoVehiculoEntity newTipo = TipoVehiculoEntity.builder()
                            .nombreTipo(TIPO_MOTO)
                            .activo(true)
                            .build();
                    return tipoVehiculoRepository.save(newTipo);
                });

        entity.setPlaca(placa);
        entity.setIdMarca(req.idMarca());
        entity.setIdTipoVehiculo(tipoMoto.getId());
        entity.setKilometrajeActual(req.kilometrajeActual());
        entity.setBelongsTo(req.belongsTo());
        if (req.idUbicacionBase() != null && !ubicacionRepository.existsById(req.idUbicacionBase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ubicación no válida");
        }
        entity.setUbicacionBase(req.idUbicacionBase() != null
                ? ubicacionRepository.getReferenceById(req.idUbicacionBase())
                : null);
        entity.setActivo(req.activo() != null ? req.activo() : entity.getActivo());
        if (req.fuelTankCapacityGallons() != null) {
            entity.setFuelTankCapacityGallons(req.fuelTankCapacityGallons());
        }
        if (req.factoryEfficiencyKmPerGallon() != null) {
            entity.setFactoryEfficiencyKmPerGallon(req.factoryEfficiencyKmPerGallon());
        }
        if (req.factoryEfficiencyUnit() != null) {
            entity.setFactoryEfficiencyUnit(req.factoryEfficiencyUnit());
        }
        vehicleRepository.save(entity);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("La motocicleta " + entity.getPlaca() + " ha sido editada por " + userPrincipal.username());
        } else {
            saveActionUseCase.save("La motocicleta " + entity.getPlaca() + " ha sido editada");
        }

        // Recalcular alertas de inmediato: el kilometraje pudo haber cambiado al editar.
        preventiveAlertCalculationService.calculateAndEmitAlerts();

        return VehicleMapper.toResponse(
                vehicleRepository.findById(entity.getIdVehiculo()).orElse(entity));
    }

    @Override
    @Transactional
    public void deleteMoto(Integer id) {
        VehicleEntity entity = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Moto no encontrada"));
        assertMotoTipo(entity);
        entity.setActivo(false);
        vehicleRepository.save(entity);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("La motocicleta " + entity.getPlaca() + " ha sido eliminada por " + userPrincipal.username());
        } else {
            saveActionUseCase.save("La motocicleta " + entity.getPlaca() + " ha sido eliminada");
        }
    }

    @Transactional
    public VehicleResponse restoreMoto(Integer id) {
        VehicleEntity moto = vehicleRepository.findDeletedById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Motocicleta eliminada no encontrada con id: " + id
                ));

        assertMotoTipo(moto);
        moto.setActivo(true);
        vehicleRepository.save(moto);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("La motocicleta " + moto.getPlaca() + " ha sido restaurada por " + userPrincipal.username());
        } else {
            saveActionUseCase.save("La motocicleta " + moto.getPlaca() + " ha sido restaurada");
        }

        return VehicleMapper.toResponse(
                vehicleRepository.findById(moto.getIdVehiculo()).orElse(moto));
    }

    private void assertMotoTipo(VehicleEntity entity) {
        if (entity.getTipoVehiculo() == null
                || !TIPO_MOTO.equalsIgnoreCase(entity.getTipoVehiculo().getNombreTipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El registro no es una motocicleta");
        }
    }
}
