package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelPerformanceResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelPerformanceUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FuelPerformanceService implements GetFuelPerformanceUseCase {

    private static final String MAQUINARIA = "MAQUINARIA";
    private static final String VEHICULO = "VEHICULO";
    private static final String MOTOCICLETA = "MOTOCICLETA";

    // Con menos de 2 desviaciones previas del propio activo, la desviación estándar
    // no tiene sentido estadístico (o queda en 0) — se usa la tolerancia fija hasta
    // entonces. Con 2 ya es matemáticamente calculable, y a partir de ahí se vuelve
    // más precisa sola con cada tanqueo nuevo, sin necesidad de configurar nada.
    private static final int MINIMO_DESVIACIONES_PREVIAS_PARA_APRENDER = 2;

    private final RefuelingRecordsRepository refuelingRecordsRepository;
    private final AssetFuelConfigRepository assetFuelConfigRepository;
    private final VehicleRepository vehicleRepository;
    private final MachineRepository machineRepository;

    @Value("${app.fuel.rendimiento-desviacion-tolerancia-porcentaje:0.08}")
    private BigDecimal toleranciaFija;

    // Techo absoluto para el modo "aprendido" — ver su uso en calcularFilasDeActivo.
    @Value("${app.fuel.rendimiento-desviacion-tolerancia-maxima-porcentaje:0.13}")
    private BigDecimal toleranciaMaximaAprendida;

    // Cuántas desviaciones estándar del propio historial del activo se toleran antes
    // de marcar alerta, una vez hay suficiente historial para calcularlas (ver
    // MINIMO_DESVIACIONES_PREVIAS_PARA_APRENDER).
    @Value("${app.fuel.rendimiento-desviacion-multiplicador-aprendido:1.5}")
    private double multiplicadorAprendido;

    @Override
    public List<FuelPerformanceResponse> obtenerRendimiento(String tipo, LocalDate fechaInicio, LocalDate fechaFin) {
        if (!MAQUINARIA.equals(tipo) && !VEHICULO.equals(tipo) && !MOTOCICLETA.equals(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tipo debe ser MAQUINARIA, VEHICULO o MOTOCICLETA.");
        }
        FechaRangoUtil.Rango rango = FechaRangoUtil.resolver(fechaInicio, fechaFin);
        boolean esMaquina = MAQUINARIA.equals(tipo);

        List<RefuelingRecordsEntity> candidatos = esMaquina
                ? refuelingRecordsRepository.findByMachineIdIsNotNullAndFechaRegistroBetween(rango.inicio(), rango.fin())
                : filtrarPorTipoVehiculo(
                        refuelingRecordsRepository.findByVehicleIdIsNotNullAndFechaRegistroBetween(rango.inicio(), rango.fin()),
                        tipo);

        Set<Long> idsCandidatos = candidatos.stream().map(RefuelingRecordsEntity::getId).collect(Collectors.toSet());
        // Ojo: un operador ternario mezclando Long (machineId) e Integer (vehicleId)
        // hace que Java promueva ambos a Long (conversión numérica binaria del
        // propio lenguaje) — se fuerza (Object) en cada rama para que cada valor
        // conserve su tipo real y el cast de más abajo no reviente.
        Map<Object, List<RefuelingRecordsEntity>> candidatosPorActivo = candidatos.stream()
                .collect(Collectors.groupingBy(t -> esMaquina ? (Object) t.getMachineId() : (Object) t.getVehicleId()));

        List<FuelPerformanceResponse> resultado = new ArrayList<>();
        for (Object activoId : candidatosPorActivo.keySet()) {
            resultado.addAll(calcularFilasDeActivo(activoId, esMaquina, idsCandidatos));
        }
        return resultado;
    }

    private List<RefuelingRecordsEntity> filtrarPorTipoVehiculo(List<RefuelingRecordsEntity> tanqueos, String tipo) {
        boolean esMoto = MOTOCICLETA.equals(tipo);
        return tanqueos.stream()
                .filter(t -> vehicleRepository.findById(t.getVehicleId())
                        .map(VehicleEntity::getTipoVehiculo)
                        .map(tv -> esMoto == "MOTOCICLETA".equalsIgnoreCase(tv.getNombreTipo()))
                        .orElse(false))
                .toList();
    }

    /**
     * Recorre TODO el historial cronológico del activo (no solo el rango filtrado —
     * el aprendizaje debe mirar toda su vida) calculando la desviación relativa de
     * cada tanqueo consecutivo, y devuelve una fila de respuesta solo para los que
     * están en {@code idsCandidatos}. Para cada uno, la alerta se decide con las
     * desviaciones de tanqueos ESTRICTAMENTE ANTERIORES en el tiempo (nunca datos
     * futuros), así la alerta que se le mostró al usuario en su momento sigue
     * siendo reproducible sin importar qué pase después.
     */
    private List<FuelPerformanceResponse> calcularFilasDeActivo(Object activoId, boolean esMaquina, Set<Long> idsCandidatos) {
        Optional<AssetFuelConfigEntity> config = esMaquina
                ? assetFuelConfigRepository.findByMachineId((Long) activoId)
                : assetFuelConfigRepository.findByVehicleId((Integer) activoId);
        // Config vieja/inválida (consumoEstandar<=0 nunca debería poder guardarse
        // desde AssetFuelConfigService, pero se revisa igual aquí como defensa):
        // dividir por cero rompería todo el cálculo de este activo, no solo una fila.
        if (config.isEmpty() || config.get().getConsumoEstandar().compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        BigDecimal consumoEstandar = config.get().getConsumoEstandar();

        String identificacionActivo = esMaquina
                ? machineRepository.findById((Long) activoId).map(MachineEntity::getName).orElse(null)
                : vehicleRepository.findById((Integer) activoId).map(this::placaConMarca).orElse(null);

        List<RefuelingRecordsEntity> historial = esMaquina
                ? refuelingRecordsRepository.findByMachineIdOrderByFechaRegistroAsc((Long) activoId)
                : refuelingRecordsRepository.findByVehicleIdOrderByFechaRegistroAsc((Integer) activoId);

        List<FuelPerformanceResponse> filas = new ArrayList<>();
        List<Double> desviacionesPrevias = new ArrayList<>();
        RefuelingRecordsEntity anterior = null;

        for (RefuelingRecordsEntity tanqueo : historial) {
            if (anterior == null) {
                anterior = tanqueo; // primer tanqueo de la serie: sin línea base, no se proyecta
                continue;
            }

            BigDecimal horometroAnterior = anterior.getHorometroKm();
            BigDecimal horometroActual = tanqueo.getHorometroKm();
            BigDecimal ejecutado = horometroActual.subtract(horometroAnterior);
            BigDecimal proyectado = FuelConsumptionProjectionUtil.proyectar(ejecutado, consumoEstandar);
            BigDecimal diferencia = tanqueo.getCantidadGalones().subtract(proyectado);
            // Un horómetro/km que retrocede (dato mal digitado o corregido hacia atrás)
            // siempre se marca como alerta de forma explícita, sin pasar por ningún
            // umbral estadístico.
            boolean horometroRetrocedio = ejecutado.signum() < 0;

            // Desviación relativa (proporcional a lo proyectado, no en galones crudos) —
            // comparable entre tanqueos de distinta magnitud del mismo activo. Queda
            // indefinida si proyectado=0 (ej. ejecutado=0 con unidad "_POR_HORA"); ese
            // punto no aporta al aprendizaje y, si es la fila evaluada, cae al 15% fijo.
            Double desviacionRelativa = proyectado.compareTo(BigDecimal.ZERO) != 0
                    ? diferencia.divide(proyectado, 6, RoundingMode.HALF_UP).doubleValue()
                    : null;

            if (idsCandidatos.contains(tanqueo.getId())) {
                boolean usaRangoAprendido = !horometroRetrocedio && desviacionRelativa != null
                        && desviacionesPrevias.size() >= MINIMO_DESVIACIONES_PREVIAS_PARA_APRENDER;

                boolean alerta;
                if (horometroRetrocedio) {
                    alerta = true;
                } else if (usaRangoAprendido) {
                    double promedio = promedio(desviacionesPrevias);
                    double desviacionEstandar = desviacionEstandarMuestral(desviacionesPrevias, promedio);
                    boolean fueraDeSuPropioRango = Math.abs(desviacionRelativa - promedio) > multiplicadorAprendido * desviacionEstandar;
                    // Techo absoluto: un activo con mucha variabilidad histórica podría
                    // "aprender" una banda demasiado laxa (ej. ±25%) donde una desviación
                    // grande igual pasa desapercibida por ser "normal" para él. Este límite
                    // no depende del historial de nadie — nunca se deja pasar una desviación
                    // mayor a este porcentaje, sin importar qué tan aprendido esté el activo.
                    boolean superaTechoAbsoluto = Math.abs(desviacionRelativa) > toleranciaMaximaAprendida.doubleValue();
                    alerta = fueraDeSuPropioRango || superaTechoAbsoluto;
                } else {
                    alerta = diferencia.abs().compareTo(proyectado.abs().multiply(toleranciaFija)) > 0;
                }

                filas.add(new FuelPerformanceResponse(
                        tanqueo.getId(), tanqueo.getVehicleId(), tanqueo.getMachineId(), tanqueo.getFuelTypeId(),
                        tanqueo.getFechaRegistro().toLocalDateTime(),
                        horometroAnterior, horometroActual, ejecutado, consumoEstandar,
                        proyectado, tanqueo.getCantidadGalones(), diferencia, alerta, usaRangoAprendido,
                        identificacionActivo, tanqueo.getEsFull()));
            }

            if (desviacionRelativa != null) {
                desviacionesPrevias.add(desviacionRelativa);
            }
            anterior = tanqueo;
        }
        return filas;
    }

    private double promedio(List<Double> valores) {
        return valores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    // Desviación estándar MUESTRAL (denominador n-1) — correcta para estimar a
    // partir de una muestra pequeña, que es justo el caso con pocos tanqueos.
    private double desviacionEstandarMuestral(List<Double> valores, double promedio) {
        double sumaCuadrados = valores.stream().mapToDouble(v -> Math.pow(v - promedio, 2)).sum();
        return Math.sqrt(sumaCuadrados / (valores.size() - 1));
    }

    private String placaConMarca(VehicleEntity vehicle) {
        String marca = vehicle.getMarca() != null ? vehicle.getMarca().getDescripcion() : null;
        return marca != null ? vehicle.getPlaca() + " — " + marca : vehicle.getPlaca();
    }
}
