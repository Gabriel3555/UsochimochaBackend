package com.app.usochicamochabackend.mapper;

import com.app.usochicamochabackend.order.application.dto.OrderWithoutInspectionResponse;
import com.app.usochicamochabackend.order.infrastructure.entity.OrderEntity;
import com.app.usochicamochabackend.performance.infrastructure.entity.ResultEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderMapperTest {

    @Test
    @DisplayName("toDtoWithoutInspection: null devuelve null")
    void toDtoWithoutInspection_Null() {
        assertNull(OrderMapper.toDtoWithoutInspection(null));
    }

    @Test
    @DisplayName("toDtoWithoutInspection: sin resultado → timeSpent null")
    void toDtoWithoutInspection_SinResultado() {
        OrderEntity order = OrderEntity.builder()
                .id(1L).status("Pending").consecutive("OT-MQ-00001").build();

        OrderWithoutInspectionResponse dto = OrderMapper.toDtoWithoutInspection(order);

        assertNotNull(dto);
        assertEquals("Pending", dto.status());
        assertEquals("OT-MQ-00001", dto.consecutive());
        assertNull(dto.timeSpent());
        assertNull(dto.hoursSpent());
        assertNull(dto.minutesSpent());
    }

    @Test
    @DisplayName("toDtoWithoutInspection: con resultado numérico → timeSpent formateado")
    void toDtoWithoutInspection_ConResultadoNumerico() {
        ResultEntity result = new ResultEntity();
        result.setHoursSpent(2);
        result.setMinutesSpent(30);

        OrderEntity order = OrderEntity.builder()
                .id(2L).status("Completada").consecutive("OT-MQ-00002").result(result).build();

        OrderWithoutInspectionResponse dto = OrderMapper.toDtoWithoutInspection(order);

        assertEquals("2h 30m", dto.timeSpent());
        assertEquals(2, dto.hoursSpent());
        assertEquals(30, dto.minutesSpent());
    }

    @Test
    @DisplayName("toDtoWithoutInspection: resultado numérico solo horas → '3h'")
    void toDtoWithoutInspection_SoloHoras() {
        ResultEntity result = new ResultEntity();
        result.setHoursSpent(3);
        result.setMinutesSpent(0);

        OrderEntity order = OrderEntity.builder()
                .id(3L).status("Completada").result(result).build();

        OrderWithoutInspectionResponse dto = OrderMapper.toDtoWithoutInspection(order);
        assertEquals("3h", dto.timeSpent());
    }

    @Test
    @DisplayName("toDtoWithoutInspection: resultado legacy string → timeSpent con valor legacy")
    void toDtoWithoutInspection_ConLegacyTimeSpent() {
        ResultEntity result = new ResultEntity();
        result.setTimeSpent("45 minutos");

        OrderEntity order = OrderEntity.builder()
                .id(4L).status("Completada").result(result).build();

        OrderWithoutInspectionResponse dto = OrderMapper.toDtoWithoutInspection(order);
        assertEquals("45 minutos", dto.timeSpent());
    }

    @Test
    @DisplayName("toDtoWithoutInspection: resultado 0h 0m → timeSpent null")
    void toDtoWithoutInspection_CeroHorasCeroMinutos() {
        ResultEntity result = new ResultEntity();
        result.setHoursSpent(0);
        result.setMinutesSpent(0);

        OrderEntity order = OrderEntity.builder()
                .id(5L).status("Completada").result(result).build();

        OrderWithoutInspectionResponse dto = OrderMapper.toDtoWithoutInspection(order);
        assertNull(dto.timeSpent());
    }
}
