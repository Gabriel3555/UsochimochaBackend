package com.app.usochicamochabackend.update.infrastructure.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OilTypeTest {

    // Regresión: el orden original de los .replace() en fromString() eliminaba
    // la propia palabra "HYDRAULIC"/"HIDRAULICO" del valor normalizado antes de
    // comparar contra ella, por lo que cualquier entrada de tipo hidráulico
    // terminaba resolviendo silenciosamente a MOTOR.
    @ParameterizedTest
    @ValueSource(strings = {"HYDRAULIC", "hydraulic", "hidraulico", "HIDRAULICO", "aceite hidraulico", "Aceite Hidraulico"})
    void fromString_reconoceValoresHidraulicos(String value) {
        assertThat(OilType.fromString(value)).isEqualTo(OilType.HYDRAULIC);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOTOR", "motor", "aceite motor", "Aceite Motor"})
    void fromString_reconoceValoresDeMotor(String value) {
        assertThat(OilType.fromString(value)).isEqualTo(OilType.MOTOR);
    }

    @Test
    void fromString_lanzaExcepcionConValorNulo() {
        assertThatThrownBy(() -> OilType.fromString(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromString_lanzaExcepcionConValorEnBlanco() {
        assertThatThrownBy(() -> OilType.fromString("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromString_lanzaExcepcionConValorNoReconocido() {
        assertThatThrownBy(() -> OilType.fromString("diesel")).isInstanceOf(IllegalArgumentException.class);
    }
}
