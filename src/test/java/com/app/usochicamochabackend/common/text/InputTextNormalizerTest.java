package com.app.usochicamochabackend.common.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputTextNormalizerTest {

    @Test
    @DisplayName("normalizePlaca: convierte a mayúsculas y elimina espacios")
    void normalizePlaca_CasoNormal() {
        assertEquals("ABC123", InputTextNormalizer.normalizePlaca("abc 123"));
        assertEquals("ABC123", InputTextNormalizer.normalizePlaca("  ABC123  "));
        assertEquals("XYZ999", InputTextNormalizer.normalizePlaca("xyz999"));
    }

    @Test
    @DisplayName("normalizePlaca: null devuelve cadena vacía")
    void normalizePlaca_Null() {
        assertEquals("", InputTextNormalizer.normalizePlaca(null));
    }

    @Test
    @DisplayName("normalizeTitleWords: capitaliza primera letra de cada palabra")
    void normalizeTitleWords_CasoNormal() {
        assertEquals("Distrito De Riego", InputTextNormalizer.normalizeTitleWords("distrito de riego"));
        assertEquals("Usochicamocha", InputTextNormalizer.normalizeTitleWords("USOCHICAMOCHA"));
    }

    @Test
    @DisplayName("normalizeTitleWords: null o en blanco devuelve null")
    void normalizeTitleWords_NullBlank() {
        assertNull(InputTextNormalizer.normalizeTitleWords(null));
        assertNull(InputTextNormalizer.normalizeTitleWords("   "));
    }

    @Test
    @DisplayName("normalizeIdCode: elimina espacios y convierte a mayúsculas; null si queda vacío")
    void normalizeIdCode_CasoNormal() {
        assertEquals("ENG001", InputTextNormalizer.normalizeIdCode(" eng 001 "));
        assertNull(InputTextNormalizer.normalizeIdCode(""));
        assertNull(InputTextNormalizer.normalizeIdCode(null));
    }

    @Test
    @DisplayName("normalizeFreeTextPreserveCase: conserva mayúsculas/minúsculas, colapsa espacios")
    void normalizeFreeTextPreserveCase_CasoNormal() {
        assertEquals("15W-40 sintético", InputTextNormalizer.normalizeFreeTextPreserveCase("  15W-40   sintético  "));
        assertNull(InputTextNormalizer.normalizeFreeTextPreserveCase("   "));
    }
}
