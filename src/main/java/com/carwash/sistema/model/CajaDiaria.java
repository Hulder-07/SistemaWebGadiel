package com.carwash.sistema.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "caja_diaria")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CajaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    @Column
    private LocalDateTime fechaCierre;

    @Column(precision = 10, scale = 2)
    private BigDecimal montoInicial = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal montoFinal = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalEfectivo = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalYape = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalTarjeta = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalDia = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal diferencia = BigDecimal.ZERO;

    // ABIERTA, CERRADA
    @Column(nullable = false, length = 20)
    private String estado = "ABIERTA";

    @ManyToOne
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // Relación con registros de lavado
    @OneToMany(mappedBy = "cajaDiaria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistroLavado> registros = new ArrayList<>();

    // Cantidad total de vehículos atendidos
    @Column
    private Integer cantidadVehiculos = 0;

    @PrePersist
    public void prePersist() {
        if (fechaApertura == null) {
            fechaApertura = LocalDateTime.now();
        }
    }

    // Métodos de conveniencia
    public void calcularTotales() {
        this.totalEfectivo = registros.stream()
            .filter(r -> "EFECTIVO".equals(r.getMetodoPago()) && "PAGADO".equals(r.getEstado()))
            .map(RegistroLavado::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalYape = registros.stream()
            .filter(r -> "YAPE".equals(r.getMetodoPago()) && "PAGADO".equals(r.getEstado()))
            .map(RegistroLavado::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalTarjeta = registros.stream()
            .filter(r -> "TARJETA".equals(r.getMetodoPago()) && "PAGADO".equals(r.getEstado()))
            .map(RegistroLavado::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalDia = totalEfectivo.add(totalYape).add(totalTarjeta);
        this.cantidadVehiculos = (int) registros.stream()
            .filter(r -> "PAGADO".equals(r.getEstado()))
            .count();
    }

    public void cerrarCaja(BigDecimal montoFinalContado) {
        this.fechaCierre = LocalDateTime.now();
        this.estado = "CERRADA";
        this.montoFinal = montoInicial.add(totalEfectivo);
        this.diferencia = montoFinalContado.subtract(this.montoFinal);
    }

    public boolean estaAbierta() {
        return "ABIERTA".equals(this.estado);
    }

    public boolean estaCerrada() {
        return "CERRADA".equals(this.estado);
    }
}