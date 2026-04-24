package com.carwash.sistema.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehiculos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String placa;

    @Column(length = 80)
    private String marca;

    @Column(length = 80)
    private String modelo;

    @Column(length = 20)
    private String color;

    // sedan, suv, camioneta, moto, bus, etc.
    @Column(length = 50)
    private String tipo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
}
