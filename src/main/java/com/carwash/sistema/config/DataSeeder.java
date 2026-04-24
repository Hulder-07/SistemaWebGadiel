package com.carwash.sistema.config;

import com.carwash.sistema.model.Servicio;
import com.carwash.sistema.repository.ServicioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedServiciosCatalog(ServicioRepository repo) {
        return args -> {
            // ── VEHICULOS ──
            add(repo, "Lavado Básico", "Lavado externo, chasis, limpieza de salón con aire...", "basico", 30, 0, 15, 20, 25);
            add(repo, "Lavado Ejecutivo", "Lavado básico + Encerado", "completo", 45, 0, 40, 50, 60);
            add(repo, "Lavado De Motor Básico", "Limpieza básica del motor", "especial", 20, 0, 65, 75, 100);
            add(repo, "Lavado De Motor Especial", "Limpieza profunda del motor", "especial", 40, 0, 150, 200, 250);
            add(repo, "Lavado De Motor Extremo", "Limpieza completa del motor", "especial", 60, 0, 300, 350, 400);
            add(repo, "Lavado De Motor A Vapor", "Limpieza con vapor a alta temperatura", "especial", 90, 0, 450, 500, 550);
            add(repo, "Pulverizado De Chasis", "Protección y limpieza del chasis", "especial", 20, 0, 20, 30, 40);
            add(repo, "Engrase De Puntos", "Lubricación de puntos móviles (por punto)", "basico", 15, 0, 5, 5, 5);
            add(repo, "Encerado", "Aplicación de cera protectora", "detailing", 30, 0, 25, 35, 45);
            add(repo, "Descontaminación De Pintura", "Lavado básico / Descontaminación / Encerado", "detailing", 60, 0, 80, 100, 120);

            // ── LIMPIEZA DE SALON (premium) ──
            add(repo, "Lavado De Salón", "Servicio adicional", "premium", 30, 0, 100, 100, 100);
            add(repo, "Lavado De Techo", "Servicio adicional", "premium", 30, 0, 100, 100, 100);
            add(repo, "Lavado De Asientos", "Servicio adicional", "premium", 30, 0, 100, 100, 100);
            add(repo, "Lavado De Alfombra", "Servicio adicional", "premium", 30, 0, 40, 40, 40);
            add(repo, "Lavado De Cinturones", "Servicio adicional", "premium", 30, 0, 40, 40, 40);
            add(repo, "Lavado De Puertas", "Servicio adicional", "premium", 30, 0, 20, 20, 20);
            add(repo, "Lavado De Tablero", "Servicio adicional", "premium", 30, 0, 20, 20, 20);
            add(repo, "Limpieza De Rejilla De Ventilación", "Servicio adicional", "premium", 30, 0, 50, 50, 50);
            add(repo, "Descontaminación De Salón A Vapor", "Servicio adicional", "premium", 30, 0, 50, 50, 50);

            // ── MOTOS ──
            add(repo, "Lavado Básico (Moto)", "Lavado de caja, champiñón...", "moto", 30, 15, 0, 0, 0);
            add(repo, "Lavado Ejecutivo (Moto)", "Lavado de caja, champiñón...", "moto", 30, 20, 0, 0, 0);
            add(repo, "Lavado Vip (Moto)", "Lavado ejecutivo / Encerado...", "moto", 30, 25, 0, 0, 0);
            add(repo, "Lavado Extremo (Moto)", "Retiro de óxido, sarro...", "moto", 30, 100, 0, 0, 0);
        };
    }

    private void add(ServicioRepository repo, String nombre, String desc, String cat, Integer dur, double base, double a, double c, double v) {
        boolean exists = repo.findAll().stream().anyMatch(s -> s.getNombre().equalsIgnoreCase(nombre));
        if (!exists) {
            Servicio s = new Servicio();
            s.setNombre(nombre);
            s.setDescripcion(desc);
            s.setCategoria(cat);
            s.setDuracionMinutos(dur);
            s.setPrecio(BigDecimal.valueOf(base));
            if (a > 0) s.setPrecioAuto(BigDecimal.valueOf(a));
            if (c > 0) s.setPrecioCamioneta(BigDecimal.valueOf(c));
            if (v > 0) s.setPrecioVan(BigDecimal.valueOf(v));
            s.setActivo(true);
            repo.save(s);
        }
    }
}
