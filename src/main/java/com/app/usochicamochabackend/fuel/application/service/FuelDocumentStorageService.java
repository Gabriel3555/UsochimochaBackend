package com.app.usochicamochabackend.fuel.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

/**
 * Almacena facturas de combustible bajo {@code uploads/documents/fuel/{subfolder}/{referenceId}/current.ext}.
 * Réplica de VehicleDocumentStorageService, parametrizando el subfolder en vez de hardcodearlo a "vehicles".
 */
@Service
public class FuelDocumentStorageService {

    private static final long MAX_BYTES = 15 * 1024 * 1024L;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");

    private final Path uploadsRoot;

    public FuelDocumentStorageService(
            @Value("${app.storage.uploads-root:uploads}") String uploadsRootProperty) {
        this.uploadsRoot = Paths.get(uploadsRootProperty).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, String subfolder, Long referenceId) throws IOException {
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
        Path targetDir = uploadsRoot
                .resolve("documents")
                .resolve("fuel")
                .resolve(subfolder)
                .resolve(String.valueOf(referenceId));
        Files.createDirectories(targetDir);

        Path target = targetDir.resolve("current" + ext);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/uploads/documents/fuel/" + subfolder + "/" + referenceId + "/current" + ext;
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
}
