package com.app.usochicamochabackend.vehicleinspection.application.service;

import com.app.usochicamochabackend.vehicleinspection.application.dto.StoredDocumentFileDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Almacena documentos bajo {@code uploads/documents/vehicles/{id}/{tipo}/current.ext}.
 * Al reemplazar, mueve el archivo {@code current} anterior a {@code archive/} sin comprimir.
 */
@Service
public class VehicleDocumentStorageService {

    private static final long MAX_BYTES = 15 * 1024 * 1024L;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");

    private final Path uploadsRoot;

    public VehicleDocumentStorageService(
            @Value("${app.storage.uploads-root:uploads}") String uploadsRootProperty) {
        this.uploadsRoot = Paths.get(uploadsRootProperty).toAbsolutePath().normalize();
    }

    public StoredDocumentFileDTO store(MultipartFile file, int idVehiculo, String tipoFolderSegment) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacío.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido (15 MB).");
        }
        String mime = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_TYPES.contains(mime)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Use JPEG, PNG, WebP o PDF.");
        }
        String ext = resolveExtension(file.getOriginalFilename(), mime);
        Path tipoDir = uploadsRoot
                .resolve("documents")
                .resolve("vehicles")
                .resolve(String.valueOf(idVehiculo))
                .resolve(tipoFolderSegment);
        Files.createDirectories(tipoDir);

        String previousArchivedRelative = null;
        try (Stream<Path> stream = Files.list(tipoDir)) {
            Path archiveDir = tipoDir.resolve("archive");
            for (Path p : stream.toList()) {
                if (Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).startsWith("current.")) {
                    Files.createDirectories(archiveDir);
                    String stamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
                    Path archivedName = archiveDir.resolve(stamp + "_" + p.getFileName());
                    Files.move(p, archivedName, StandardCopyOption.REPLACE_EXISTING);
                    previousArchivedRelative = "/uploads/documents/vehicles/" + idVehiculo
                            + "/" + tipoFolderSegment + "/archive/" + archivedName.getFileName();
                }
            }
        }

        Path target = tipoDir.resolve("current" + ext);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String relative = "/uploads/documents/vehicles/" + idVehiculo + "/" + tipoFolderSegment + "/current" + ext;
        return new StoredDocumentFileDTO(relative, mime, previousArchivedRelative);
    }

    private static String resolveExtension(String originalFilename, String mime) {
        String fromName = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fromName = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        if (fromName.length() <= 5 && fromName.matches("\\.(jpe?g|png|webp|pdf)")) {
            return fromName;
        }
        return switch (mime) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> ".bin";
        };
    }

    /** Segmento de carpeta sin espacios (p. ej. LICENCIA_DE_CONDUCCION). */
    public static String folderSegmentForTipoBd(String tipoBdNormalizado) {
        return tipoBdNormalizado.trim().replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
