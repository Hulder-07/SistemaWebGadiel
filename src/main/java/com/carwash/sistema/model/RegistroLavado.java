package com.carwash.sistema.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_lavado")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegistroLavado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "caja_diaria_id", nullable = false)
    private CajaDiaria cajaDiaria;

    // Número correlativo del día
    @Column(nullable = false)
    private Integer numeroOrden;

    // MOTO, AUTO, CAMIONETA, SUV, CAMION
    @Column(nullable = false, length = 50)
    private String tipoVehiculo;

    @Column(length = 10)
    private String placa;

    // Referencia al servicio del catálogo (opcional)
    @ManyToOne
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    // Descripción del servicio (si no usa catálogo)
    @Column(length = 200)
    private String servicioDescripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    // EFECTIVO, YAPE, TARJETA
    @Column(nullable = false, length = 20)
    private String metodoPago;

    // PENDIENTE, PAGADO, CANCELADO
    @Column(nullable = false, length = 20)
    private String estado = "PAGADO";

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne
    @JoinColumn(name = "usuario_registro_id")
    private Usuario usuarioRegistro;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // Cliente (opcional)
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    // Método para obtener descripción completa del servicio
    public String getServicioCompleto() {
        if (servicio != null) {
            return servicio.getNombre();
        }
        return servicioDescripcion != null ? servicioDescripcion : "Sin descripción";
    }

    // Método para obtener ícono del método de pago
    public String getIconoMetodoPago() {
        return switch (metodoPago) {
            case "EFECTIVO" -> "💵";
            case "YAPE" -> "📱";
            case "TARJETA" -> "💳";
            default -> "💰";
        };
    }

    // Método para obtener ícono del tipo de vehículo
    public String getIconoVehiculo() {
        return switch (tipoVehiculo) {
            case "MOTO" -> "🏍️";
            case "AUTO" -> "🚗";
            case "CAMIONETA" -> "🚙";
            case "SUV" -> "🚘";
            case "CAMION" -> "🚛";
            default -> "🚗";
        };
    }
}