package com.carwash.sistema.controller;

import com.carwash.sistema.repository.OrdenRepository;
import com.carwash.sistema.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class NotificacionController {

    private final OrdenRepository ordenRepository;
    private final ProductoRepository productoRepository;

    @GetMapping("/api/notificaciones")
    public Map<String, Object> notificaciones() {
        List<Map<String, Object>> lista = new ArrayList<>();

        // Órdenes pendientes
        long pendientes = ordenRepository.countByEstado("PENDIENTE");
        if (pendientes > 0) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("tipo", "alerta");
            n.put("icono", "fa-car");
            n.put("titulo", pendientes + " orden(es) pendiente(s)");
            n.put("mensaje", "Hay vehículos esperando ser atendidos");
            n.put("url", "/ordenes");
            lista.add(n);
        }

        // Stock bajo
        productoRepository.findBajoStock().forEach(p -> {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("tipo", "peligro");
            n.put("icono", "fa-box");
            n.put("titulo", "Stock bajo: " + p.getNombre());
            n.put("mensaje", "Stock actual: " + p.getStock() + " (mínimo: " + p.getStockMinimo() + ")");
            n.put("url", "/productos");
            lista.add(n);
        });

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total", lista.size());
        res.put("notificaciones", lista);
        return res;
    }
}
