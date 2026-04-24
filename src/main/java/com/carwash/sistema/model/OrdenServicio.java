package com.carwash.sistema.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ordenes_servicio")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrdenServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // PENDIENTE, EN_PROCESO, COMPLETADO, CANCELADO
    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "numero_orden", length = 30)
    private String numeroOrden;

    @Column(length = 255)
    private String observaciones;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    @Column
    private LocalDateTime fechaFin;

    @PrePersist
    public void prePersist() {
        if (fechaInicio == null) fechaInicio = LocalDateTime.now();
        if (total == null && servicio != null) total = servicio.getPrecio();
        if (numeroOrden == null) numeroOrden = "ORD-" + System.currentTimeMillis();
    }
}
