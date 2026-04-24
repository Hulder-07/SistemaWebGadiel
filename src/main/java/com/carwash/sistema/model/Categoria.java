package com.carwash.sistema.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "categorias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 50)
    private String icono;

    @Column(length = 7)
    private String color;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    // EAGER para que Thymeleaf pueda acceder sin sesión JPA abierta
    @OneToMany(mappedBy = "categoria", fetch = FetchType.EAGER)
    private List<Producto> productos;
}