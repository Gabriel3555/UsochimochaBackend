package com.app.usochicamochabackend.catalog.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cat_tipos_vehiculo")
public class TipoVehiculoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_vehiculo")
    private Integer id;

    @Column(name = "nombre_tipo")
    private String nombreTipo;

    private Boolean activo;
}
