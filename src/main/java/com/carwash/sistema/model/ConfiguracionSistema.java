package com.carwash.sistema.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_sistema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ConfiguracionSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ══════════════════════════════════════════════════════════
    // DATOS DEL NEGOCIO
    // ══════════════════════════════════════════════════════════
    
    @Column(length = 200)
    private String nombreNegocio = "Car Detailing Gadiel";
    
    @Column(length = 11)
    private String ruc = "20615184421";
    
    @Column(length = 300)
    private String direccion = "";
    
    @Column(length = 20)
    private String telefono = "";
    
    @Column(length = 100)
    private String email = "";
    
    @Column(length = 500)
    private String logoUrl;

    // ══════════════════════════════════════════════════════════
    // SISTEMA - PERSONALIZACIÓN
    // ══════════════════════════════════════════════════════════
    
    @Column(length = 20)
    private String colorPrimario = "#dc2626";  // Rojo
    
    @Column(length = 20)
    private String colorSecundario = "#1f2937";  // Gris oscuro
    
    @Column(length = 20)
    private String tema = "claro";  // claro, oscuro, auto
    
    @Column(length = 20)
    private String tamanioTexto = "mediano";  // pequeño, mediano, grande

    // ══════════════════════════════════════════════════════════
    // HORA E IDIOMA
    // ══════════════════════════════════════════════════════════
    
    @Column(length = 50)
    private String zonaHoraria = "America/Lima";
    
    @Column(length = 20)
    private String formatoFecha = "DD/MM/YYYY";
    
    @Column(length = 10)
    private String formatoHora = "24h";  // 24h, 12h
    
    @Column(length = 10)
    private String idioma = "es";  // es, en
    
    @Column(length = 10)
    private String moneda = "PEN";  // PEN, USD

    // ══════════════════════════════════════════════════════════
    // FACTURACIÓN
    // ══════════════════════════════════════════════════════════
    
    @Column(length = 200)
    private String nubefactToken = "";
    
    @Column(length = 20)
    private String nubefactModo = "beta";  // beta, produccion
    
    @Column(length = 10)
    private String serieBoleta = "B001";
    
    @Column(length = 10)
    private String serieFactura = "F001";
    
    @Column
    private Boolean facturacionActiva = false;

    // ══════════════════════════════════════════════════════════
    // CAJA LAVADERO
    // ══════════════════════════════════════════════════════════
    
    @Column(precision = 10, scale = 2)
    private BigDecimal montoInicialCaja = new BigDecimal("0.00");
    
    @Column
    private Boolean efectivoActivo = true;
    
    @Column
    private Boolean yapeActivo = true;
    
    @Column
    private Boolean tarjetaActiva = true;

    // ══════════════════════════════════════════════════════════
    // NOTIFICACIONES
    // ══════════════════════════════════════════════════════════
    
    @Column(length = 100)
    private String emailAlertas = "";
    
    @Column(length = 20)
    private String whatsappAlertas = "";
    
    @Column
    private Boolean notificacionesActivas = true;

    // ══════════════════════════════════════════════════════════
    // RENIEC
    // ══════════════════════════════════════════════════════════
    
    @Column(length = 200)
    private String reniecToken = "";
    
    @Column
    private Boolean reniecActivo = true;

    // ══════════════════════════════════════════════════════════
    // COPIAS DE SEGURIDAD
    // ══════════════════════════════════════════════════════════
    
    @Column
    private Boolean backupAutomatico = false;
    
    @Column(length = 20)
    private String frecuenciaBackup = "diario";  // diario, semanal, mensual
    
    @Column
    private LocalDateTime ultimoBackup;

    // ══════════════════════════════════════════════════════════
    // DISPOSITIVOS
    // ══════════════════════════════════════════════════════════
    
    @Column(length = 100)
    private String impresoraTermica = "";
    
    @Column
    private Boolean impresoraConectada = false;
    
    @Column(length = 100)
    private String tabletCajero = "";
    
    @Column
    private Boolean tabletConectada = false;

    // ══════════════════════════════════════════════════════════
    // SISTEMA
    // ══════════════════════════════════════════════════════════
    
    @Column(length = 20)
    private String version = "1.0.0";
    
    @Column
    private LocalDateTime fechaActualizacion;
    
    @Column
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}