package com.carwash.sistema.service;

import com.carwash.sistema.model.Producto;
import com.carwash.sistema.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {
    private final ProductoRepository productoRepository;
    public List<Producto> listarActivos() { return productoRepository.findByActivoTrue(); }
    public List<Producto> listarBajoStock() { return productoRepository.findBajoStock(); }
    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id).orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }
    public Producto guardar(Producto p) { return productoRepository.save(p); }
    public void desactivar(Long id) {
        productoRepository.findById(id).ifPresent(p -> { p.setActivo(false); productoRepository.save(p); });
    }
}
