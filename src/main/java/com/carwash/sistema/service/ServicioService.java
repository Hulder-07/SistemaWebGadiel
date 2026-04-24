package com.carwash.sistema.service;

import com.carwash.sistema.model.Servicio;
import com.carwash.sistema.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicioService {
    private final ServicioRepository servicioRepository;
    public List<Servicio> listarActivos() { return servicioRepository.findByActivoTrue(); }
    public List<Servicio> listarTodos() { return servicioRepository.findAll(); }
    public Servicio buscarPorId(Long id) {
        return servicioRepository.findById(id).orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
    }
    public Servicio guardar(Servicio s) { return servicioRepository.save(s); }
    public void desactivar(Long id) {
        servicioRepository.findById(id).ifPresent(s -> { s.setActivo(false); servicioRepository.save(s); });
    }
}
