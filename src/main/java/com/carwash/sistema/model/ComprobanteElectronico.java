package com.carwash.sistema.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comprobantes_electronicos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ComprobanteElectronico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "BOLETA" o "FACTURA"
    @Column(nullable = false, length = 20)
    private String tipoComprobante;

    // ✅ NUEVO: Serie del comprobante (B001, F001, etc.)
    @Column(length = 30)
    private String serie;

    // ✅ NUEVO: Número correlativo (1, 2, 3, etc.)
    @Column
    private Integer numero;

    // Número completo (B001-123)
    @Column(length = 20)
    private String numeroCompleto;

    @Column(length = 150)
    private String clienteNombre;

    @Column(length = 15)
    private String clienteDocumento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(precision = 10, scale = 2)
    private BigDecimal igv;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    // PENDIENTE, ACEPTADO, RECHAZADO, ERROR
    @Column(length = 20)
    private String estado = "PENDIENTE";

    @Column(length = 500)
    private String urlPdf;

    @Column(length = 500)
    private String urlXml;

    @Column(length = 500)
    private String urlCdr;

    @Column(length = 500)
    private String mensajeError;

    @Column(nullable = false)
    private LocalDateTime fechaEmision;

    // Referencia a orden o venta
    @Column(name = "orden_id")
    private Long ordenId;

    @Column(name = "venta_id")
    private Long ventaId;

    @PrePersist
    public void prePersist() {
        if (fechaEmision == null) fechaEmision = LocalDateTime.now();
    }
}
