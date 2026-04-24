package com.carwash.sistema.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "clientes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 8, unique = true)
    private String dni;

    @Column(length = 150)
    private String nombres;

    @Column(length = 100)
    private String apellidoPaterno;

    @Column(length = 100)
    private String apellidoMaterno;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(length = 255)
    private String direccion;

    @Column
    private LocalDate fechaNacimiento;

    @Column(nullable = false)
    private Boolean activo = true;

    // Método helper para obtener nombre completo
    @Transient
    public String getNombreCompleto() {
        StringBuilder nombreCompleto = new StringBuilder();
        
        if (nombres != null && !nombres.isEmpty()) {
            nombreCompleto.append(nombres);
        }
        
        if (apellidoPaterno != null && !apellidoPaterno.isEmpty()) {
            if (nombreCompleto.length() > 0) nombreCompleto.append(" ");
            nombreCompleto.append(apellidoPaterno);
        }
        
        if (apellidoMaterno != null && !apellidoMaterno.isEmpty()) {
            if (nombreCompleto.length() > 0) nombreCompleto.append(" ");
            nombreCompleto.append(apellidoMaterno);
        }
        
        return nombreCompleto.toString();
    }

    // Para compatibilidad con código antiguo (getter de "nombre")
    @Transient
    public String getNombre() {
        return getNombreCompleto();
    }
}
