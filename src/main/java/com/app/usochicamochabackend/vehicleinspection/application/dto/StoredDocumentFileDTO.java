package com.app.usochicamochabackend.vehicleinspection.application.dto;

/** Resultado de almacenar un archivo bajo uploads/documents/vehicles/... */
public record StoredDocumentFileDTO(
        String relativeUrl,
        String contentType,
        /** Ruta relativa del archivo anterior tras ser movido a archive/; null si no había archivo previo. */
        String previousArchivedUrl
) {}
