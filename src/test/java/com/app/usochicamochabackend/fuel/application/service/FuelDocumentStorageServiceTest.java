package com.app.usochicamochabackend.fuel.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FuelDocumentStorageServiceTest {

    @TempDir
    Path tempUploadsRoot;

    private FuelDocumentStorageService fuelDocumentStorageService;

    @BeforeEach
    void setUp() {
        fuelDocumentStorageService = new FuelDocumentStorageService(tempUploadsRoot.toString());
    }

    @Test
    void archivoMayorA15MB_LanzaIllegalArgumentException() {
        byte[] contenido = new byte[16 * 1024 * 1024];
        MockMultipartFile archivo = new MockMultipartFile("factura", "grande.pdf", "application/pdf", contenido);

        assertThrows(IllegalArgumentException.class,
                () -> fuelDocumentStorageService.store(archivo, "purchases", 1L));
    }

    @Test
    void tipoDeArchivoNoPermitido_LanzaIllegalArgumentException() {
        MockMultipartFile archivo = new MockMultipartFile("factura", "factura.txt", "text/plain", new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class,
                () -> fuelDocumentStorageService.store(archivo, "purchases", 1L));
    }

    @Test
    void archivoValido_GuardaYDevuelveRutaConPrefijoEsperado() throws IOException {
        MockMultipartFile archivo = new MockMultipartFile("factura", "f.pdf", "application/pdf", new byte[]{1, 2, 3});

        String ruta = fuelDocumentStorageService.store(archivo, "purchases", 7L);

        assertEquals("/uploads/documents/fuel/purchases/7/current.pdf", ruta);
        assertTrue(Files.exists(tempUploadsRoot.resolve("documents/fuel/purchases/7/current.pdf")));
    }

    @Test
    void archivoVacio_LanzaIllegalArgumentException() {
        MockMultipartFile archivo = new MockMultipartFile("factura", "vacio.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> fuelDocumentStorageService.store(archivo, "refueling", 1L));
    }
}
