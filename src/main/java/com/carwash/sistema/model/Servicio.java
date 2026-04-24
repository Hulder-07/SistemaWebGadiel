package com.carwash.sistema.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "servicios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal precioAuto;

    @Column(precision = 10, scale = 2)
    private BigDecimal precioCamioneta;

    @Column(precision = 10, scale = 2)
    private BigDecimal precioVan;

    // basico, completo, premium, detailing, especial, moto

    @Column(length = 50)
    private String categoria;

    // minutos estimados
    @Column
    private Integer duracionMinutos;

    @Column(nullable = false)
    private Boolean activo = true;
}
