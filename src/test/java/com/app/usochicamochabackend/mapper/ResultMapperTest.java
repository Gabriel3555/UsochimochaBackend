package com.app.usochicamochabackend.mapper;

import com.app.usochicamochabackend.performance.application.dto.ResultDTO;
import com.app.usochicamochabackend.performance.infrastructure.entity.ResultEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultMapperTest {

    @Test
    @DisplayName("toResponseResult: null devuelve null")
    void toResponseResult_Null() {
        assertNull(ResultMapper.toResponseResult(null));
    }

    @Test
    @DisplayName("toResponseResult: horas y minutos → timeSpent formateado")
    void toResponseResult_ConHorasMinutos() {
        ResultEntity entity = new ResultEntity();
        entity.setHoursSpent(1);
        entity.setMinutesSpent(45);

        ResultDTO dto = ResultMapper.toResponseResult(entity);

        assertEquals("1h 45m", dto.timeSpent());
    }

    @Test
    @DisplayName("toResponseResult: solo minutos → '20m'")
    void toResponseResult_SoloMinutos() {
        ResultEntity entity = new ResultEntity();
        entity.setHoursSpent(0);
        entity.setMinutesSpent(20);

        ResultDTO dto = ResultMapper.toResponseResult(entity);
        assertEquals("20m", dto.timeSpent());
    }

    @Test
    @DisplayName("toResponseResult: ambos null → fallback a legacy string")
    void toResponseResult_LegacyFallback() {
        ResultEntity entity = new ResultEntity();
        entity.setTimeSpent("30 minutos");

        ResultDTO dto = ResultMapper.toResponseResult(entity);
        assertEquals("30 minutos", dto.timeSpent());
    }

    @Test
    @DisplayName("toResponseResult: ambos null sin legacy → timeSpent null")
    void toResponseResult_SinDatos() {
        ResultEntity entity = new ResultEntity();

        ResultDTO dto = ResultMapper.toResponseResult(entity);
        assertNull(dto.timeSpent());
    }

    @Test
    @DisplayName("toResponseList: null devuelve null")
    void toResponseList_Null() {
        assertNull(ResultMapper.toResponseList(null));
    }

    @Test
    @DisplayName("toResponseList: lista vacía devuelve lista vacía")
    void toResponseList_Vacia() {
        assertTrue(ResultMapper.toResponseList(java.util.List.of()).isEmpty());
    }
}
