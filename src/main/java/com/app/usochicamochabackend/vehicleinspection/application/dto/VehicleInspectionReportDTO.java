package com.app.usochicamochabackend.vehicleinspection.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
        name = "VehicleInspectionReportDTO",
        description = "Vista de una inspección pre-operativa completa (vehículo liviano/pesado): cabecera, mecánico, documentos, elementos y salud. "
                        + "Usada en reportes por tipo y en historiales de moto filtrados por tipo MOTOCICLETA.")
public record VehicleInspectionReportDTO(
        @Schema(description = "PK `inspeccion_pre_operativa`") Long idInspeccion,
        @Schema(description = "Marca temporal del registro") LocalDateTime fechaRegistro,
        @Schema(description = "Placa del vehículo inspeccionado") String placa,
        @Schema(description = "Marca (texto)") String marca,
        @Schema(description = "Tipo de vehículo") String tipoVehiculo,
        @Schema(description = "Área organizacional / belongs_to") String areaOrganizacional,
        @Schema(description = "Nombre de ubicación o estación si aplica") String ubicacion,
        @Schema(description = "Usuario inspector (login)") String responsable,
        @Schema(description = "Kilometraje reportado en la inspección") Integer kilometraje,
        @Schema(description = "Si la ruta fue aprobada") Boolean aprobadoRuta,
        @Schema(description = "Observaciones finales") String observacionesFinales,

        // Detalle Mecánico
        @Schema(description = "Chequeo nivel aceite") String nivelAceite,
        @Schema(description = "Chequeo refrigerante") String nivelRefrigerante,
        @Schema(description = "Chequeo frenos") String nivelFrenos,
        @Schema(description = "Estado llantas") String estadoLlantas,
        @Schema(description = "Luces en general") String lucesGeneral,
        @Schema(description = "Estado visual general") String estadoVisual,
        @Schema(description = "Limpieza") String limpiezaGeneral,

        // Detalle Documentos (Checks visuales)
        @Schema(description = "Estado SOAT declarado") String checkSoat,
        @Schema(description = "Estado Tecnomecánica declarado") String checkTecno,
        @Schema(description = "Estado extintor declarado") String checkExtintor,

        // Detalle Elementos
        @Schema(description = "Tiene botiquín") Boolean tieneBotiquin,
        @Schema(description = "Tiene señalización") Boolean tieneSeñalizacion,
        @Schema(description = "Tiene líneas de emergencia (mapeo interno a extintor en persistencia histórica)")
        Boolean tieneLineasEmergencia,
        @Schema(description = "Llanta de repuesto") String tieneLlantaRepuesto,
        @Schema(description = "Gato hidráulico") String tieneGatoHidraulico,

        // Detalle Salud
        @Schema(description = "Condición física conductor") Boolean saludFisica,
        @Schema(description = "Condición mental") Boolean saludMental,
        @Schema(description = "Sobriedad") Boolean sobrio,
        @Schema(description = "Medicamentos") Boolean medicamentos,
        @Schema(description = "Condición para conducir") Boolean condicionParaConducir,
        @Schema(description = "Consciencia de responsabilidad") Boolean conscienteResponsabilidad,

        // Detalle Mano de Obra
        @Schema(description = "Nombre del mecánico de planta que realizó el procedimiento") String mechanicName,
        @Schema(description = "Nombre del contratista que realizó el procedimiento") String contractorName,
        @Schema(description = "Tiempo empleado en el procedimiento") String timeSpent
) {}
